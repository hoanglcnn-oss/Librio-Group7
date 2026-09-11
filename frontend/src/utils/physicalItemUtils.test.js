import test from 'node:test'
import assert from 'node:assert'
import { fetchResourcePhysicalItems } from './physicalItemUtils.js'

test('fetchResourcePhysicalItems does not make requests if resource title is empty', async () => {
  let called = false
  const fetchPageFn = async () => { called = true; return {} }
  const result = await fetchResourcePhysicalItems({ id: 1 }, fetchPageFn)
  assert.deepStrictEqual(result, [])
  assert.strictEqual(called, false)
})

test('fetchResourcePhysicalItems fetches with size=100 and aggregates pages matching resource.id', async () => {
  const resource = { id: 123, title: 'Java Book' }
  const apiCalls = []
  
  const mockPages = [
    {
      items: [
        { id: 10, resource: { id: 123 } }, // match
        { id: 11, resource: { id: 999 } }, // no match
        { id: 12, resource: { id: 123 } }  // match
      ],
      totalPages: 3
    },
    {
      items: [
        { id: 13, resource: { id: 123 } }, // match
      ],
      totalPages: 3
    },
    {
      items: [
        { id: 14, resource: { id: 999 } }, // no match
      ],
      totalPages: 3
    }
  ]
  
  const fetchPageFn = async (params) => {
    apiCalls.push(params)
    return mockPages[params.page]
  }

  const result = await fetchResourcePhysicalItems(resource, fetchPageFn)

  assert.strictEqual(apiCalls.length, 3, 'Should fetch 3 pages')
  assert.deepStrictEqual(apiCalls[0], { q: 'Java Book', size: 100, page: 0 })
  assert.deepStrictEqual(apiCalls[1], { q: 'Java Book', size: 100, page: 1 })
  assert.deepStrictEqual(apiCalls[2], { q: 'Java Book', size: 100, page: 2 })

  assert.strictEqual(result.length, 3, 'Should aggregate exactly 3 matching items')
  assert.strictEqual(result[0].id, 10)
  assert.strictEqual(result[1].id, 12)
  assert.strictEqual(result[2].id, 13)
})
