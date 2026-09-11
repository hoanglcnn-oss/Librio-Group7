import { API_BASE_URL } from '../config/runtime'

export async function lookupBookMetadata(isbn) {
  const url = `${API_BASE_URL}/librarian/book-metadata/lookup?isbn=${encodeURIComponent(isbn)}`
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
    },
  })

  if (!response.ok) {
    if (response.status === 400) throw new Error('INVALID_ISBN')
    if (response.status === 404) throw new Error('BOOK_METADATA_NOT_FOUND')
    if (response.status === 504) throw new Error('BOOK_METADATA_LOOKUP_TIMEOUT')
    if (response.status === 502) throw new Error('BOOK_METADATA_PROVIDER_ERROR')
    throw new Error('UNKNOWN_ERROR')
  }

  return response.json()
}
