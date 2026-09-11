import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Header from '../components/Header'
import PhysicalItemAdmin from '../components/PhysicalItemAdmin'
import { createLibrarianResource, getLibrarianResource, updateLibrarianResource } from '../services/authApi'
import { lookupBookMetadata } from '../services/bookMetadataApi'
import { emptyResourceForm, resourceFormToPayload, resourceToForm, validateResourceForm } from '../utils/resourceForm'
import './BookMetadataLookup.css'

function ResourceAdminPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyResourceForm)
  const [persistedTitle, setPersistedTitle] = useState('')
  const [managedResource, setManagedResource] = useState(null)
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState(editing ? 'loading' : 'ready')
  const [message, setMessage] = useState('')

  const [lookupStatus, setLookupStatus] = useState('idle')
  const [lookupError, setLookupError] = useState('')

  const isMountedRef = useRef(true)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  async function handleLookup(event) {
    event.preventDefault()
    if (!form.isbn) {
      setLookupError('Vui lòng nhập ISBN để tra cứu.')
      setLookupStatus('error')
      return
    }
    setLookupStatus('loading')
    setLookupError('')
    try {
      const meta = await lookupBookMetadata(form.isbn.trim())
      if (isMountedRef.current) {
        setForm(current => ({
          ...current,
          title: meta.title || current.title,
          authors: (meta.authors && meta.authors.length > 0) ? meta.authors.join(', ') : current.authors,
          description: meta.description || current.description,
          isbn: meta.isbn || current.isbn,
          coverImageUrl: meta.coverImageUrl || current.coverImageUrl,
          externalSourceId: meta.externalSourceId || current.externalSourceId,
          metadataSource: 'GOOGLE_BOOKS'
        }))
        setLookupStatus('success')
      }
    } catch (err) {
      if (isMountedRef.current) {
        setLookupStatus('error')
        const msgMap = {
          'INVALID_ISBN': 'Mã ISBN không hợp lệ.',
          'BOOK_METADATA_NOT_FOUND': 'Không tìm thấy thông tin sách.',
          'BOOK_METADATA_LOOKUP_TIMEOUT': 'Quá thời gian tra cứu, vui lòng thử lại.',
          'BOOK_METADATA_PROVIDER_ERROR': 'Lỗi từ dịch vụ cung cấp metadata.'
        }
        setLookupError(msgMap[err.message] || 'Lỗi tra cứu không xác định.')
      }
    }
  }

  // Full hydration for initial load and after a successful resource save.
  const loadResource = useCallback(async () => {
    if (!editing || !id) return
    try {
      const resource = await getLibrarianResource(id)
      if (isMountedRef.current) {
        setManagedResource(resource)
        setForm(resourceToForm(resource))
        setPersistedTitle(resource.title)
        setStatus('ready')
      }
    } catch (error) {
      if (isMountedRef.current) {
        setMessage(error.message)
        setStatus('error')
      }
    }
  }, [editing, id])

  // Snapshot refresh after a physical‑item mutation – only updates server‑derived data.
  const refreshManagedResourceSnapshot = useCallback(async () => {
    if (!editing || !id) return
    try {
      const resource = await getLibrarianResource(id)
      if (isMountedRef.current) {
        setManagedResource(resource)
        // Do NOT modify form or persistedTitle.
      }
    } catch (error) {
      if (isMountedRef.current) {
        setMessage(error.message)
        setStatus('error')
      }
    }
  }, [editing, id])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadResource()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadResource])

  function updateField(event) {
    const { name, type, checked, value } = event.target
    setForm(current => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setErrors(current => ({ ...current, [name]: undefined, ...(name.startsWith('has') ? { accessTypes: undefined } : {}) }))
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
      if (isMountedRef.current) {
        setManagedResource(saved)
        setForm(resourceToForm(saved))
        setPersistedTitle(saved.title)
        setStatus('saved')
        setMessage(editing ? 'Đã lưu thay đổi tài liệu.' : `Đã tạo tài liệu #${saved.id}.`)
      }
      if (!editing) navigate(`/librarian/resources/${saved.id}/edit`, { replace: true })
    } catch (error) {
      if (isMountedRef.current) {
        setStatus('error')
        const friendlyMessage = error.message === 'RESOURCE_ISBN_EXISTS'
          ? 'Mã ISBN này đã tồn tại trong hệ thống.'
          : error.message
        setMessage(friendlyMessage)
      }
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
            {form.metadataSource === 'GOOGLE_BOOKS' && (
              <div style={{ marginBottom: '16px' }}>
                <span className="metadata-source-badge">Nguồn: GOOGLE_BOOKS</span>
              </div>
            )}

            <FormField label="ISBN (Tùy chọn)" hint="Tra cứu thông tin tự động từ Google Books bằng mã ISBN" error={errors.isbn}>
              <div className="isbn-lookup-group">
                <input name="isbn" value={form.isbn || ''} maxLength={20} onChange={updateField} aria-invalid={Boolean(errors.isbn)} />
                <button type="button" className="secondary-action" onClick={handleLookup} disabled={lookupStatus === 'loading'}>
                  {lookupStatus === 'loading' ? 'Đang tra...' : 'Tra cứu'}
                </button>
              </div>
              {lookupStatus === 'error' && <div className="isbn-lookup-status error">{lookupError}</div>}
              {lookupStatus === 'success' && <div className="isbn-lookup-status success">Tra cứu thành công! Dữ liệu đã được điền.</div>}
            </FormField>

            <FormField label="URL Ảnh bìa (Tùy chọn)" error={errors.coverImageUrl}>
              <input name="coverImageUrl" value={form.coverImageUrl || ''} maxLength={1000} onChange={updateField} aria-invalid={Boolean(errors.coverImageUrl)} />
              {form.coverImageUrl && (
                <div className="cover-preview">
                  <span className="cover-preview-label">Ảnh bìa:</span>
                  <img src={form.coverImageUrl} alt="Cover preview" onError={(e) => e.target.style.display = 'none'} />
                </div>
              )}
            </FormField>

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
              <label className="checkbox-card"><input name="hasPhysical" type="checkbox" checked={form.hasPhysical} onChange={updateField} />
                <span><strong>Bản vật lý</strong><small>Quản lý số lượng bản sách.</small></span>
              </label>
              <label className="checkbox-card"><input name="hasDigital" type="checkbox" checked={form.hasDigital} onChange={updateField} />
                <span><strong>Tài liệu số</strong><small>Cho phép gắn nội dung số được bảo vệ.</small></span>
              </label>
              {errors.accessTypes && <small className="field-error">{errors.accessTypes}</small>}
            </fieldset>

            {form.hasPhysical && (
              <>
                <FormField label="Tổng số bản vật lý" error={errors.physicalCopies} required>
                  <input name="physicalCopies" type="number" min="1" max="9999" step="1" value={form.physicalCopies} onChange={updateField} aria-invalid={Boolean(errors.physicalCopies)} />
                </FormField>
                {managedResource?.physical && (
                  <div className="availability-summary-info" style={{ marginTop: '0.25rem', marginBottom: '1rem', color: 'var(--text-secondary, #4a5568)' }}>
                    <small>Trạng thái khả dụng (từ hệ thống): <strong>{managedResource.physical.availableCopies} / {managedResource.physical.totalCopies}</strong> bản có thể mượn</small>
                  </div>
                )}
              </>
            )}

            {message && status !== 'error' && <div className="demo-success" role="status"><strong>{message}</strong></div>}
            <div className="resource-form-actions">
              <Link className="text-action" to="/resources">Hủy</Link>
              <button className="primary-action" type="submit" disabled={status === 'saving'}>{status === 'saving' ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo tài liệu'}</button>
            </div>
          </form>
        )}

        {editing && status !== 'loading' && status !== 'error' && (
          <PhysicalItemAdmin resource={{ id: Number(id), title: persistedTitle }} onItemChange={refreshManagedResourceSnapshot} />
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
