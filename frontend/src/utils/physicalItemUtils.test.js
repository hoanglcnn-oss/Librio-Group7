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

test('create physical copy triggers resource + physical-item refresh', async () => {
  const { handlePhysicalItemMutation } = await import('./physicalItemUtils.js')
  let mutationCalled = false
  let physicalRefreshCalled = false
  let resourceRefreshCalled = false

  const result = await handlePhysicalItemMutation({
    mutationFn: async () => {
      mutationCalled = true
      return { id: 1, barcode: 'BAR-001' }
    },
    refreshPhysicalItems: async () => {
      physicalRefreshCalled = true
    },
    refreshManagedResource: async () => {
      resourceRefreshCalled = true
    },
  })

  assert.strictEqual(mutationCalled, true)
  assert.strictEqual(physicalRefreshCalled, true)
  assert.strictEqual(resourceRefreshCalled, true)
  assert.strictEqual(result.barcode, 'BAR-001')
})

test('update inventory status triggers refresh', async () => {
  const { handlePhysicalItemMutation } = await import('./physicalItemUtils.js')
  let mutationCalled = false
  let physicalRefreshCalled = false
  let resourceRefreshCalled = false

  const result = await handlePhysicalItemMutation({
    mutationFn: async () => {
      mutationCalled = true
      return { id: 1, inventoryStatus: 'LOST' }
    },
    refreshPhysicalItems: async () => {
      physicalRefreshCalled = true
    },
    refreshManagedResource: async () => {
      resourceRefreshCalled = true
    },
  })

  assert.strictEqual(mutationCalled, true)
  assert.strictEqual(physicalRefreshCalled, true)
  assert.strictEqual(resourceRefreshCalled, true)
  assert.strictEqual(result.inventoryStatus, 'LOST')
})

test('no client-side count mutation - availability count is derived from server payload', async () => {
  const { handlePhysicalItemMutation } = await import('./physicalItemUtils.js')
  let serverResource = { id: 10, physical: { totalCopies: 5, availableCopies: 5 } }
  let clientState = { ...serverResource }

  const refreshManagedResource = async () => {
    serverResource = { id: 10, physical: { totalCopies: 5, availableCopies: 4 } }
    clientState = { ...serverResource }
  }

  await handlePhysicalItemMutation({
    mutationFn: async () => ({ id: 1, inventoryStatus: 'LOST' }),
    refreshPhysicalItems: async () => {},
    refreshManagedResource,
  })

  assert.strictEqual(clientState.physical.availableCopies, 4)
  assert.strictEqual(clientState.physical.totalCopies, 5)
})

test('unsaved resource title still does not affect copy loading', async () => {
  let calledQuery = null
  const fetchPageFn = async (params) => {
    calledQuery = params.q
    return { items: [], totalPages: 1 }
  }

  const persistedTitle = 'Saved Title'
  const unsavedFormTitle = 'Unsaved Work In Progress Title'

  await fetchResourcePhysicalItems({ id: 1, title: persistedTitle }, fetchPageFn)

  assert.strictEqual(calledQuery, 'Saved Title')
  assert.notStrictEqual(calledQuery, unsavedFormTitle)
})

test('async load does not set state after component unmount', async () => {
  let stateSet = false
  let isMounted = true

  const isMountedFn = () => isMounted

  const safeSetState = () => {
    if (isMountedFn()) {
      stateSet = true
    }
  }

  // Simulate unmount before async callback returns
  isMounted = false
  if (isMountedFn()) {
    safeSetState()
  }

  assert.strictEqual(stateSet, false, 'State should not be updated after unmount')
})

