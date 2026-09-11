import { useCallback, useEffect, useRef, useState } from 'react'
import { ERROR_MESSAGES, createPhysicalItem, getLibrarianPhysicalItems, updatePhysicalItem } from '../services/authApi'
import { fetchResourcePhysicalItems, handlePhysicalItemMutation } from '../utils/physicalItemUtils'

export default function PhysicalItemAdmin({ resource, onItemChange }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  const [formVisible, setFormVisible] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  
  const [barcode, setBarcode] = useState('')
  const [location, setLocation] = useState('')
  const [inventoryStatus, setInventoryStatus] = useState('ACTIVE')
  
  const [formError, setFormError] = useState('')
  const [formStatus, setFormStatus] = useState('ready') // ready, saving, saved
  const [successMsg, setSuccessMsg] = useState('')

  const isMountedRef = useRef(true)

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  const resourceId = resource?.id
  const resourceTitle = resource?.title

  const loadItems = useCallback(async () => {
    if (!resourceTitle) return
    setLoading(true)
    setLoadError('')
    try {
      const allMatches = await fetchResourcePhysicalItems({ id: resourceId, title: resourceTitle }, getLibrarianPhysicalItems)
      if (isMountedRef.current) {
        setItems(allMatches)
      }
    } catch (e) {
      if (isMountedRef.current) {
        setLoadError(e.message)
      }
    } finally {
      if (isMountedRef.current) {
        setLoading(false)
      }
    }
  }, [resourceId, resourceTitle])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadItems()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadItems])

  function openCreate() {
    setEditingItem(null)
    setBarcode('')
    setLocation('')
    setInventoryStatus('ACTIVE')
    setFormError('')
    setSuccessMsg('')
    setFormStatus('ready')
    setFormVisible(true)
  }

  function openEdit(item) {
    setEditingItem(item)
    setBarcode(item.barcode || '')
    setLocation(item.location || '')
    setInventoryStatus(item.inventoryStatus || 'ACTIVE')
    setFormError('')
    setSuccessMsg('')
    setFormStatus('ready')
    setFormVisible(true)
  }

  function closeForm() {
    setFormVisible(false)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError('')
    setSuccessMsg('')
    
    if (!barcode.trim()) {
      setFormError('Mã vạch là bắt buộc')
      return
    }
    if (!location.trim()) {
      setFormError('Vị trí là bắt buộc')
      return
    }

    setFormStatus('saving')
    try {
      const isEdit = Boolean(editingItem)
      const barcodeValue = barcode.trim()
      const locationValue = location.trim()

      await handlePhysicalItemMutation({
        mutationFn: () =>
          isEdit
            ? updatePhysicalItem(editingItem.id, { barcode: barcodeValue, location: locationValue, inventoryStatus })
            : createPhysicalItem(resourceId, { barcode: barcodeValue, location: locationValue }),
        refreshPhysicalItems: loadItems,
        refreshManagedResource: onItemChange,
      })

      if (isMountedRef.current) {
        setSuccessMsg(isEdit ? `Đã lưu thay đổi cho bản sách ${barcodeValue}` : `Đã tạo bản sách ${barcodeValue}`)
        setFormStatus('saved')
        setFormVisible(false)
      }
    } catch (err) {
      if (isMountedRef.current) {
        setFormStatus('error')
        let msg = err.message
        if (err.code && ERROR_MESSAGES[err.code]) {
          msg = ERROR_MESSAGES[err.code]
        }
        setFormError(msg)
      }
    }
  }

  const INVENTORY_STATUSES = [
    { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
    { value: 'LOST', label: 'Đã mất (LOST)' },
    { value: 'DAMAGED', label: 'Hư hỏng (DAMAGED)' },
    { value: 'WITHDRAWN', label: 'Thu hồi (WITHDRAWN)' },
  ]

  const isCirculationActive = editingItem && (editingItem.circulationStatus === 'RESERVED' || editingItem.circulationStatus === 'BORROWED')

  return (
    <section className="physical-item-admin">
      <div className="physical-header">
        <h2>Quản lý bản vật lý</h2>
        <button type="button" className="secondary-action" onClick={openCreate} disabled={loading || formVisible}>
          + Thêm bản sách
        </button>
      </div>

      {loadError && <div className="demo-error" role="alert">{loadError}</div>}
      
      {loading ? (
        <p>Đang tải danh sách...</p>
      ) : (
        <div className="physical-list">
          {items.length === 0 ? (
            <p className="shelf-empty">Chưa có bản vật lý nào.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã vạch</th>
                  <th>Vị trí</th>
                  <th>Tồn kho</th>
                  <th>Lưu thông</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>{item.barcode}</td>
                    <td>{item.location}</td>
                    <td><span className={`status-tag status-${item.inventoryStatus?.toLowerCase()}`}>{item.inventoryStatus}</span></td>
                    <td><span className={`status-tag status-${item.circulationStatus?.toLowerCase()}`}>{item.circulationStatus}</span></td>
                    <td>
                      <button type="button" className="text-action" onClick={() => openEdit(item)}>Sửa</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {formVisible && (
        <form className="physical-item-form card" onSubmit={handleSubmit}>
          <h3>{editingItem ? 'Sửa bản sách' : 'Thêm bản sách mới'}</h3>
          {formError && <div className="demo-error" role="alert">{formError}</div>}
          
          <label className="resource-form-field">
            <span>Mã vạch *</span>
            <input name="barcode" value={barcode} onChange={e => setBarcode(e.target.value)} disabled={formStatus === 'saving'} />
          </label>
          <label className="resource-form-field">
            <span>Vị trí *</span>
            <input name="location" value={location} onChange={e => setLocation(e.target.value)} disabled={formStatus === 'saving'} />
          </label>
          
          {editingItem && (
            <label className="resource-form-field">
              <span>Trạng thái tồn kho</span>
              {isCirculationActive ? (
                <div>
                  <select disabled value={inventoryStatus}>
                    <option value={inventoryStatus}>{inventoryStatus}</option>
                  </select>
                  <small className="field-error">Không thể thay đổi khi sách đang được giữ hoặc mượn ({editingItem.circulationStatus})</small>
                </div>
              ) : (
                <select name="inventoryStatus" value={inventoryStatus} onChange={e => setInventoryStatus(e.target.value)} disabled={formStatus === 'saving'}>
                  {INVENTORY_STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              )}
            </label>
          )}

          <div className="resource-form-actions">
            <button type="button" className="text-action" onClick={closeForm} disabled={formStatus === 'saving'}>Hủy</button>
            <button type="submit" className="primary-action" disabled={formStatus === 'saving'}>
              {formStatus === 'saving' ? 'Đang lưu...' : 'Lưu bản sách'}
            </button>
          </div>
        </form>
      )}

      {successMsg && !formVisible && (
        <div className="demo-success" role="status" style={{ marginTop: '1rem' }}>
          <strong>{successMsg}</strong>
        </div>
      )}
    </section>
  )
}
