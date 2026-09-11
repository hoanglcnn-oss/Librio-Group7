export function parseCockpitPage(pageStr) {
  const page = parseInt(pageStr, 10)
  if (isNaN(page) || page < 0) return 0
  return page
}

export function parseCockpitSize(sizeStr) {
  const size = parseInt(sizeStr, 10)
  if (isNaN(size) || size < 1 || size > 100) return 20
  if (![20, 50, 100].includes(size)) return 20
  return size
}
