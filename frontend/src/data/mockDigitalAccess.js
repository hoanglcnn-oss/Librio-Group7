function byteLength(value) {
  return new TextEncoder().encode(value).length
}

export function createMockPdfBlob() {
  const content = 'BT /F1 22 Tf 72 740 Td (Librio Digital Library) Tj 0 -38 Td /F1 12 Tf (Authenticated PDF preview for T-125) Tj ET'
  const objects = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n',
    '4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n',
    `5 0 obj\n<< /Length ${byteLength(content)} >>\nstream\n${content}\nendstream\nendobj\n`,
  ]
  let pdf = '%PDF-1.4\n'
  const offsets = [0]
  objects.forEach((object) => {
    offsets.push(byteLength(pdf))
    pdf += object
  })
  const xrefOffset = byteLength(pdf)
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  pdf += offsets.slice(1).map((offset) => `${String(offset).padStart(10, '0')} 00000 n \n`).join('')
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`
  return new Blob([pdf], { type: 'application/pdf' })
}

export function getMockDigitalCapability(resourceId) {
  return {
    resourceId: Number(resourceId),
    canRead: true,
    contentUrl: URL.createObjectURL(createMockPdfBlob()),
    temporaryUrl: true,
  }
}
