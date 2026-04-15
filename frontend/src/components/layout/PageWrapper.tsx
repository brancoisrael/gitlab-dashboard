import './PageWrapper.css'

interface PageWrapperProps {
  title: string
  children: React.ReactNode
}

export function PageWrapper({ title, children }: PageWrapperProps) {
  return (
    <div className="page-wrapper">
      <div className="page-wrapper__header">
        <h1 className="page-wrapper__title">{title}</h1>
      </div>
      <div className="page-wrapper__content">{children}</div>
    </div>
  )
}
