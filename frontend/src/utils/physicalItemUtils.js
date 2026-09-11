export async function fetchResourcePhysicalItems(resource, fetchPageFn) {
  if (!resource?.title) return []
  
  let currentPage = 0
  let totalPages = 1
  const allMatches = []
  
  while (currentPage < totalPages) {
    const res = await fetchPageFn({ q: resource.title, size: 100, page: currentPage })
    const pageItems = res.items || res.content || []
    
    allMatches.push(...pageItems.filter(item => item.resource?.id === resource.id))
    
    totalPages = res.totalPages || 1
    currentPage += 1
  }
  
  return allMatches
}
