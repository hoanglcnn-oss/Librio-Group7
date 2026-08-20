import { useState } from 'react'

function SearchIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m21 21-4.35-4.35m2.35-5.15A7.5 7.5 0 1 1 4 11.5a7.5 7.5 0 0 1 15 0Z" /></svg>
}

function SearchInput({ initialValue, onSearch }) {
  const [value, setValue] = useState(initialValue)

  function handleSubmit(event) {
    event.preventDefault()
    onSearch(value.trim())
  }

  return (
    <form className="search-box" onSubmit={handleSubmit} role="search">
      <SearchIcon />
      <input value={value} onChange={(event) => setValue(event.target.value)} placeholder="Tìm theo tên tài liệu hoặc tác giả..." aria-label="Tìm tài nguyên" />
      <button type="submit">Tìm kiếm</button>
    </form>
  )
}

export default SearchInput
