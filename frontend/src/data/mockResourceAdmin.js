import { resources } from './mockResources'

let managedResources = resources.map((resource) => structuredClone(resource))

export async function getMockManagedResource(resourceId) {
  const resource = managedResources.find((item) => item.id === Number(resourceId))
  if (!resource) {
    const error = new Error('Không tìm thấy tài liệu cần chỉnh sửa.')
    error.status = 404
    throw error
  }
  return structuredClone(resource)
}

export async function saveMockManagedResource(payload, resourceId) {
  if (resourceId) {
    const index = managedResources.findIndex((item) => item.id === Number(resourceId))
    if (index < 0) return getMockManagedResource(resourceId)
    managedResources[index] = { ...managedResources[index], ...structuredClone(payload), id: Number(resourceId) }
    return structuredClone(managedResources[index])
  }
  const created = { ...structuredClone(payload), id: Math.max(0, ...managedResources.map((item) => item.id)) + 1 }
  managedResources.push(created)
  return structuredClone(created)
}
