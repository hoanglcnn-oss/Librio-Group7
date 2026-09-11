/**
 * T-146 regression: physical-item refresh must not overwrite unsaved resource form fields.
 *
 * Tests exercise the refresh-ownership contract at the helper level using
 * Node's built-in test runner (no DOM / React required).
 */
import assert from 'node:assert/strict'
import test from 'node:test'
import { resourceToForm } from './resourceForm.js'

function makeState() {
  return {
    form: { title: '', authors: '', description: '', category: '', hasPhysical: false, physicalCopies: '1', hasDigital: false },
    persistedTitle: '',
    managedResource: null,
  }
}

/** Full hydration: used on initial load and after a successful Resource save. */
async function loadResource(state, fetchResource) {
  const resource = await fetchResource()
  state.managedResource = resource
  state.form = resourceToForm(resource)
  state.persistedTitle = resource.title
}

/**
 * Snapshot refresh: used after a physical-item mutation.
 * Must update managedResource ONLY – must NOT touch form or persistedTitle.
 */
async function refreshManagedResourceSnapshot(state, fetchResource) {
  const resource = await fetchResource()
  state.managedResource = resource
}

const serverResource = {
  id: 1,
  title: 'Python Programming',
  authors: ['Author A'],
  description: 'Original description',
  category: 'Tech',
  accessTypes: ['PHYSICAL'],
  physical: { totalCopies: 3, availableCopies: 3 },
  digital: null,
}

const serverResourceAfterItemMutation = {
  ...serverResource,
  physical: { totalCopies: 3, availableCopies: 2 },
}

test('T-146: loadResource hydrates form, managedResource, and persistedTitle', async () => {
  const state = makeState()
  await loadResource(state, async () => serverResource)

  assert.equal(state.form.title, 'Python Programming')
  assert.equal(state.persistedTitle, 'Python Programming')
  assert.equal(state.managedResource.physical.availableCopies, 3)
})

test('T-146: refreshManagedResourceSnapshot updates availability without resetting unsaved form edits', async () => {
  const state = makeState()
  await loadResource(state, async () => serverResource)

  state.form.title = 'Unsaved edit in progress'
  state.form.description = 'New unsaved description'

  await refreshManagedResourceSnapshot(state, async () => serverResourceAfterItemMutation)

  assert.equal(
    state.managedResource.physical.availableCopies,
    2,
    'managedResource.physical.availableCopies should reflect server state after item mutation',
  )
  assert.equal(
    state.form.title,
    'Unsaved edit in progress',
    'form.title must NOT be overwritten by refreshManagedResourceSnapshot',
  )
  assert.equal(
    state.form.description,
    'New unsaved description',
    'form.description must NOT be overwritten by refreshManagedResourceSnapshot',
  )
  assert.equal(
    state.persistedTitle,
    'Python Programming',
    'persistedTitle must NOT be overwritten by refreshManagedResourceSnapshot',
  )
})

test('T-146: loadResource after resource save correctly re-hydrates all fields', async () => {
  const state = makeState()
  await loadResource(state, async () => serverResource)

  state.form.title = 'Being edited...'

  const savedResource = { ...serverResource, title: 'Python Programming 2nd Ed' }
  await loadResource(state, async () => savedResource)

  assert.equal(state.form.title, 'Python Programming 2nd Ed')
  assert.equal(state.persistedTitle, 'Python Programming 2nd Ed')
})
