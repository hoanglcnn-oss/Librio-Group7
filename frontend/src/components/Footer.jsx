import { Link } from 'react-router-dom'

function Footer() {
  return (
    <footer id="about">
      <Link className="brand" to="/resources"><span className="brand-mark">L</span><span>Librio</span></Link>
      <p>Thư viện trường học — đồng hành cùng học tập và nghiên cứu.</p>
      <span>© 2026 Librio School Library</span>
    </footer>
  )
}

export default Footer
