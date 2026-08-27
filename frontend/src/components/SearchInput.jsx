import { useEffect, useRef, useState } from 'react'

function SearchIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m21 21-4.35-4.35m2.35-5.15A7.5 7.5 0 1 1 4 11.5a7.5 7.5 0 0 1 15 0Z" /></svg>
}

function SearchInput({ initialValue, onSearch }) {
  const [value, setValue] = useState(initialValue)
  const [pending, setPending] = useState(false)
  const lastSearch = useRef(initialValue.trim())

  useEffect(() => {
    const keyword = value.trim()
    if (keyword === lastSearch.current) {
      setPending(false)
      return undefined
    }

    setPending(true)
    const timer = window.setTimeout(() => {
      lastSearch.current = keyword
      setPending(false)
      onSearch(keyword)
    }, 1000)

    return () => window.clearTimeout(timer)
  }, [value, onSearch])

  function handleSubmit(event) {
    event.preventDefault()
    const keyword = value.trim()
    lastSearch.current = keyword
    setPending(false)
    onSearch(keyword)
  }

  return (
    <form className="search-box" onSubmit={handleSubmit} role="search">
      <SearchIcon />
      <input value={value} onChange={(event) => setValue(event.target.value)} placeholder="Tìm theo tên tài liệu hoặc tác giả..." aria-label="Tìm tài nguyên" />
      <button type="submit">{pending ? 'Đang chờ…' : 'Tìm kiếm'}</button>
    </form>
  )
}

export default SearchInput
