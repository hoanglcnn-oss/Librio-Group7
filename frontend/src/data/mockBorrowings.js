export const mockReaderBorrowings = {
  activeBorrowings: [
    {
      id: 901,
      resource: { id: 1, title: 'Clean Code' },
      borrowedAt: '2026-08-01T09:00:00+07:00',
      dueDate: '2026-08-15T09:00:00+07:00',
      overdue: true,
    },
    {
      id: 902,
      resource: { id: 2, title: 'Design Patterns' },
      borrowedAt: '2026-08-28T14:30:00+07:00',
      dueDate: '2026-09-11T14:30:00+07:00',
      overdue: false,
    },
  ],
}
