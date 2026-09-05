export const emptyResourceForm = {
  title: '',
  authors: '',
  description: '',
  category: '',
  hasPhysical: false,
  physicalCopies: '1',
  hasDigital: false,
}

export function resourceToForm(resource) {
  return {
    title: resource?.title || '',
    authors: (resource?.authors || []).join(', '),
    description: resource?.description || '',
    category: resource?.category || '',
    hasPhysical: resource?.accessTypes?.includes('PHYSICAL') || Boolean(resource?.physical),
    physicalCopies: String(resource?.physical?.totalCopies ?? 1),
    hasDigital: resource?.accessTypes?.includes('DIGITAL') || Boolean(resource?.digital),
  }
}

export function validateResourceForm(form) {
  const errors = {}
  const title = form.title.trim()
  const authors = form.authors.split(',').map((author) => author.trim()).filter(Boolean)
  const copies = Number(form.physicalCopies)

  if (!title) errors.title = 'Tên tài liệu là bắt buộc.'
  else if (title.length > 200) errors.title = 'Tên tài liệu không được vượt quá 200 ký tự.'
  if (!authors.length) errors.authors = 'Nhập ít nhất một tác giả.'
  if (form.description.length > 5000) errors.description = 'Mô tả không được vượt quá 5.000 ký tự.'
  if (!form.hasPhysical && !form.hasDigital) errors.accessTypes = 'Chọn ít nhất một loại tài liệu.'
  if (form.hasPhysical && (!Number.isInteger(copies) || copies < 1 || copies > 9999)) {
    errors.physicalCopies = 'Số bản vật lý phải là số nguyên từ 1 đến 9.999.'
  }
  return errors
}

export function resourceFormToPayload(form) {
  const accessTypes = [form.hasPhysical && 'PHYSICAL', form.hasDigital && 'DIGITAL'].filter(Boolean)
  return {
    title: form.title.trim(),
    authors: form.authors.split(',').map((author) => author.trim()).filter(Boolean),
    description: form.description.trim(),
    category: form.category.trim() || null,
    accessTypes,
    physical: form.hasPhysical ? { totalCopies: Number(form.physicalCopies) } : null,
    digital: form.hasDigital ? { available: true } : null,
  }
}
