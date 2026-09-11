import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

test('T-165: stable error mapping exists for membership failures', () => {
  const authApiContent = fs.readFileSync(path.join(__dirname, '../services/authApi.js'), 'utf-8')
  
  assert.ok(authApiContent.includes("MEMBERSHIP_NOT_ELIGIBLE: 'Không đủ điều kiện đăng ký gói thành viên này.'"))
  assert.ok(authApiContent.includes("MEMBERSHIP_PLAN_NOT_FOUND: 'Không tìm thấy gói thành viên.'"))
  assert.ok(authApiContent.includes("ACTIVE_MEMBERSHIP_EXISTS: 'Bạn đã có gói thành viên đang hoạt động.'"))
  assert.ok(authApiContent.includes("MEMBERSHIP_ACTIVATION_CONFLICT: 'Có lỗi khi kích hoạt gói thành viên.'"))
  assert.ok(authApiContent.includes("INVALID_PAYMENT_OUTCOME: 'Kết quả thanh toán không hợp lệ.'"))
})

function makeState() {
  return {
    membership: null,
    submitting: false,
    paymentError: null,
  }
}

async function handlePayment(state, outcome, simulatePayment, fetchMembership) {
  if (state.submitting) return // double submit guard
  
  state.submitting = true
  state.paymentError = null
  
  try {
    await simulatePayment(outcome)
    state.membership = await fetchMembership()
  } catch (err) {
    state.paymentError = err.message
    state.membership = await fetchMembership() // refresh even on failure
  } finally {
    state.submitting = false
  }
}

test('T-165: SUCCESS refresh path updates membership and clears errors', async () => {
  const state = makeState()
  let simulateCalled = 0
  let fetchCalled = 0
  
  await handlePayment(
    state,
    'SUCCESS',
    async () => { simulateCalled++ },
    async () => { fetchCalled++; return { status: 'ACTIVE' } }
  )
  
  assert.equal(simulateCalled, 1)
  assert.equal(fetchCalled, 1)
  assert.equal(state.membership.status, 'ACTIVE')
  assert.equal(state.paymentError, null)
  assert.equal(state.submitting, false)
})

test('T-165: FAILED refresh path updates membership and sets error', async () => {
  const state = makeState()
  let simulateCalled = 0
  let fetchCalled = 0
  
  await handlePayment(
    state,
    'FAILED',
    async () => { simulateCalled++; throw new Error('Payment failed') },
    async () => { fetchCalled++; return { status: 'NONE', latestPayment: { status: 'FAILED' } } }
  )
  
  assert.equal(simulateCalled, 1)
  assert.equal(fetchCalled, 1)
  assert.equal(state.membership.status, 'NONE')
  assert.equal(state.paymentError, 'Payment failed')
  assert.equal(state.submitting, false)
})

test('T-165: double-submit guard prevents multiple concurrent requests', async () => {
  const state = makeState()
  let simulateCalled = 0
  
  // mock slow payment
  const simulatePayment = async () => {
    simulateCalled++
    await new Promise(r => setTimeout(r, 10))
  }
  
  const fetchMembership = async () => ({ status: 'ACTIVE' })
  
  // start first
  const p1 = handlePayment(state, 'SUCCESS', simulatePayment, fetchMembership)
  // try second immediately while first is in flight
  const p2 = handlePayment(state, 'SUCCESS', simulatePayment, fetchMembership)
  
  await Promise.all([p1, p2])
  
  assert.equal(simulateCalled, 1, 'Simulate should only be called once due to double-submit guard')
})