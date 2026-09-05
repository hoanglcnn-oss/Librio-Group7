import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Header from '../components/Header'
import { createLibrarianResource, getLibrarianResource, updateLibrarianResource } from '../services/authApi'
import { emptyResourceForm, resourceFormToPayload, resourceToForm, validateResourceForm } from '../utils/resourceForm'

function ResourceAdminPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyResourceForm)
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState(editing ? 'loading' : 'ready')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!editing) return undefined
    let active = true
    getLibrarianResource(id)
      .then((resource) => { if (active) { setForm(resourceToForm(resource)); setStatus('ready') } })
      .catch((error) => { if (active) { setMessage(error.message); setStatus('error') } })
    return () => { active = false }
  }, [editing, id])

  function updateField(event) {
    const { name, type, checked, value } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setErrors((current) => ({ ...current, [name]: undefined, ...(name.startsWith('has') ? { accessTypes: undefined } : {}) }))
    setMessage('')
  }

  async function submit(event) {
    event.preventDefault()
    const nextErrors = validateResourceForm(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return

    setStatus('saving')
    setMessage('')
    try {
      const payload = resourceFormToPayload(form)
      const saved = editing
        ? await updateLibrarianResource(id, payload)
        : await createLibrarianResource(payload)
      setStatus('saved')
      setMessage(editing ? 'Đã lưu thay đổi tài liệu.' : `Đã tạo tài liệu #${saved.id}.`)
      if (!editing) navigate(`/librarian/resources/${saved.id}/edit`, { replace: true })
    } catch (error) {
      setStatus('error')
      setMessage(error.message)
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="resource-admin-page">
        <Link className="back-link" to="/resources">← Về kho tài liệu</Link>
        <section className="my-library-hero">
          <p className="eyebrow">QUẢN TRỊ TÀI NGUYÊN</p>
          <h1>{editing ? 'Chỉnh sửa tài liệu' : 'Thêm tài liệu mới'}</h1>
          <p>Cập nhật metadata và loại truy cập. Mã tài liệu được hệ thống tự tạo.</p>
        </section>

        {status === 'loading' && <div className="shelf-empty resource-form-loading">Đang tải dữ liệu…</div>}
        {status === 'error' && message && <div className="demo-error" role="alert">{message}</div>}
        {status !== 'loading' && (
          <form className="resource-admin-form" noValidate onSubmit={submit}>
            <FormField label="Tên tài liệu" error={errors.title} required>
              <input name="title" value={form.title} maxLength={200} onChange={updateField} aria-invalid={Boolean(errors.title)} />
            </FormField>
            <FormField label="Tác giả" hint="Phân cách nhiều tác giả bằng dấu phẩy." error={errors.authors} required>
              <input name="authors" value={form.authors} onChange={updateField} aria-invalid={Boolean(errors.authors)} />
            </FormField>
            <FormField label="Danh mục">
              <input name="category" value={form.category} maxLength={100} onChange={updateField} />
            </FormField>
            <FormField label="Mô tả" hint={`${form.description.length}/5000 ký tự`} error={errors.description}>
              <textarea name="description" value={form.description} maxLength={5000} rows={7} onChange={updateField} aria-invalid={Boolean(errors.description)} />
            </FormField>

            <fieldset className="access-type-fieldset">
              <legend>Loại tài liệu <span>*</span></legend>
              <label className="checkbox-card"><input name="hasPhysical" type="checkbox" checked={form.hasPhysical} onChange={updateField} /><span><strong>Bản vật lý</strong><small>Quản lý số lượng bản sách.</small></span></label>
              <label className="checkbox-card"><input name="hasDigital" type="checkbox" checked={form.hasDigital} onChange={updateField} /><span><strong>Tài liệu số</strong><small>Cho phép gắn nội dung số được bảo vệ.</small></span></label>
              {errors.accessTypes && <small className="field-error">{errors.accessTypes}</small>}
            </fieldset>

            {form.hasPhysical && (
              <FormField label="Tổng số bản vật lý" error={errors.physicalCopies} required>
                <input name="physicalCopies" type="number" min="0" max="9999" step="1" value={form.physicalCopies} onChange={updateField} aria-invalid={Boolean(errors.physicalCopies)} />
              </FormField>
            )}

            {message && status !== 'error' && <div className="demo-success" role="status"><strong>{message}</strong></div>}
            <div className="resource-form-actions">
              <Link className="text-action" to="/resources">Hủy</Link>
              <button className="primary-action" type="submit" disabled={status === 'saving'}>{status === 'saving' ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo tài liệu'}</button>
            </div>
          </form>
        )}
      </main>
      <Footer />
    </div>
  )
}

function FormField({ label, hint, error, required, children }) {
  return (
    <label className="resource-form-field">
      <span>{label}{required && <b> *</b>}</span>
      {children}
      {hint && <small>{hint}</small>}
      {error && <small className="field-error">{error}</small>}
    </label>
  )
}

export default ResourceAdminPage
