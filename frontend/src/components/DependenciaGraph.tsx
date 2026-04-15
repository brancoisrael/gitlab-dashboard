import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { DependenciaConsulta } from '../types/dependencias.types'
import './DependenciaGraph.css'

// ── Simulation constants ────────────────────────────────────────────────────
const R = 20
const LABEL_OFFSET = R + 14
const REPULSION = 6000
const LINK_DIST = 150
const LINK_STRENGTH = 0.04
const CENTER_FORCE = 0.008
const DAMPING = 0.84
const MIN_D2 = 400   // 20px mínimo — previne forças explosivas e NaN
const MAX_V = 12     // clampa velocidade para evitar explosão de posição

// ── Types ───────────────────────────────────────────────────────────────────
interface SimNode {
  id: string
  tipo: 'projeto' | 'dependencia'
  x: number; y: number
  vx: number; vy: number
  fx: number | null; fy: number | null
}

interface SimEdge { source: string; target: string }

interface Props { readonly dados: DependenciaConsulta[] }

// ── Graph builder ───────────────────────────────────────────────────────────
function initNodes(dados: DependenciaConsulta[]): { nodes: SimNode[]; edges: SimEdge[] } {
  const nm = new Map<string, SimNode>()
  const edges: SimEdge[] = []

  const add = (id: string, tipo: 'projeto' | 'dependencia') => {
    if (nm.has(id)) return
    const i = nm.size, angle = i * 2.4, r = 150 + i * 25
    nm.set(id, { id, tipo, x: Math.cos(angle) * r, y: Math.sin(angle) * r, vx: 0, vy: 0, fx: null, fy: null })
  }

  for (const item of dados) {
    if (item.tipo === 'projeto') {
      add(item.nome, 'projeto')
      for (const dep of item.lista) { add(dep.nome, 'dependencia'); edges.push({ source: item.nome, target: dep.nome }) }
    } else {
      add(item.nome, 'dependencia')
      for (const proj of item.lista) { add(proj.nome, 'projeto'); edges.push({ source: proj.nome, target: item.nome }) }
    }
  }

  return { nodes: [...nm.values()], edges }
}

// ── Simulation steps ─────────────────────────────────────────────────────────
function applyRepulsion(nodes: SimNode[]): void {
  for (let i = 0; i < nodes.length; i++) {
    for (let j = i + 1; j < nodes.length; j++) {
      const a = nodes[i], b = nodes[j]
      const dx = b.x - a.x, dy = b.y - a.y
      const d2 = Math.max(dx * dx + dy * dy, MIN_D2), d = Math.sqrt(d2)
      const f = REPULSION / d2, fx = f * dx / d, fy = f * dy / d
      if (a.fx === null) { a.vx -= fx; a.vy -= fy }
      if (b.fx === null) { b.vx += fx; b.vy += fy }
    }
  }
}

function applyLinks(edges: SimEdge[], nm: Map<string, SimNode>): void {
  for (const e of edges) {
    const s = nm.get(e.source), t = nm.get(e.target)
    if (!s || !t) continue
    const dx = t.x - s.x, dy = t.y - s.y
    const d = Math.hypot(dx, dy) || 1
    const f = (d - LINK_DIST) * LINK_STRENGTH
    const fx = f * dx / d, fy = f * dy / d
    if (s.fx === null) { s.vx += fx; s.vy += fy }
    if (t.fx === null) { t.vx -= fx; t.vy -= fy }
  }
}

function integrate(nodes: SimNode[]): void {
  for (const n of nodes) {
    if (n.fx !== null) { n.x = n.fx; n.y = n.fy!; n.vx = 0; n.vy = 0; continue }
    n.vx = (n.vx - n.x * CENTER_FORCE) * DAMPING
    n.vy = (n.vy - n.y * CENTER_FORCE) * DAMPING
    // limita velocidade para evitar explosão numérica → NaN → nós em (0,0)
    const v = Math.hypot(n.vx, n.vy)
    if (v > MAX_V) { n.vx = n.vx * MAX_V / v; n.vy = n.vy * MAX_V / v }
    n.x += n.vx; n.y += n.vy
    if (!Number.isFinite(n.x)) n.x = (Math.random() - 0.5) * 200
    if (!Number.isFinite(n.y)) n.y = (Math.random() - 0.5) * 200
  }
}

// ── SVG DOM helpers ──────────────────────────────────────────────────────────
const NS = 'http://www.w3.org/2000/svg'
function svgEl<T extends SVGElement>(tag: string): T {
  return document.createElementNS(NS, tag) as T
}
function setAttrs(el: Element, attrs: Record<string, string>): void {
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, v)
}

function buildArrowDefs(): SVGDefsElement {
  const defs = svgEl<SVGDefsElement>('defs')
  const marker = svgEl<SVGMarkerElement>('marker')
  setAttrs(marker, { id: 'dg-arrow', markerWidth: '8', markerHeight: '8', refX: String(R + 5), refY: '3', orient: 'auto' })
  const path = svgEl<SVGPathElement>('path')
  setAttrs(path, { d: 'M0,0 L0,6 L8,3 z', fill: '#94a3b8' })
  marker.appendChild(path)
  defs.appendChild(marker)
  return defs
}

function buildNodeEl(n: SimNode): SVGGElement {
  const g = svgEl<SVGGElement>('g')
  setAttrs(g, { 'data-nid': n.id, class: 'dep-graph__node' })

  const circle = svgEl<SVGCircleElement>('circle')
  setAttrs(circle, {
    r: String(R),
    fill: n.tipo === 'projeto' ? '#3b82f6' : '#22c55e',
    stroke: n.tipo === 'projeto' ? '#1d4ed8' : '#16a34a',
    'stroke-width': '2',
  })
  g.appendChild(circle)

  const text = svgEl<SVGTextElement>('text')
  setAttrs(text, {
    y: String(LABEL_OFFSET),
    'text-anchor': 'middle',
    'dominant-baseline': 'hanging',
    fill: '#334155',
    'font-size': '10',
    'font-family': 'system-ui, sans-serif',
    'pointer-events': 'none',
    'font-weight': '500',
  })
  text.textContent = n.id.length > 24 ? `${n.id.slice(0, 22)}…` : n.id
  g.appendChild(text)

  const title = svgEl<SVGElement>('title')
  title.textContent = n.id
  g.appendChild(title)

  return g
}

// ── Component ────────────────────────────────────────────────────────────────
export function DependenciaGraph({ dados }: Props) {
  const { t } = useTranslation()

  const svgRef    = useRef<SVGSVGElement>(null)
  const gRef      = useRef<SVGGElement>(null)
  const nodesRef  = useRef<SimNode[]>([])
  const edgesRef  = useRef<SimEdge[]>([])
  const nmRef     = useRef<Map<string, SimNode>>(new Map())
  const nodeElRef = useRef<Map<string, SVGGElement>>(new Map())
  const edgeElRef = useRef<Map<string, SVGLineElement>>(new Map())
  const rafRef    = useRef<number>(0)
  const txRef     = useRef({ x: 0, y: 0, k: 1 })
  const dragRef   = useRef<{ id: string; ox: number; oy: number } | null>(null)
  const panRef    = useRef<{ sx: number; sy: number; otx: number; oty: number } | null>(null)

  const [hiddenIds, setHiddenIds] = useState<Set<string>>(new Set())

  const allNodes = useMemo(() => initNodes(dados).nodes, [dados])
  const projects = useMemo(() => allNodes.filter(n => n.tipo === 'projeto'), [allNodes])
  const depNodes = useMemo(() => allNodes.filter(n => n.tipo === 'dependencia'), [allNodes])

  // ── Apply SVG transform ───────────────────────────────────────────────────
  const applyTx = useCallback(() => {
    const g = gRef.current
    if (!g) return
    const { x, y, k } = txRef.current
    g.setAttribute('transform', `translate(${x.toFixed(1)},${y.toFixed(1)}) scale(${k.toFixed(4)})`)
  }, [])

  // ── Center view when data changes ─────────────────────────────────────────
  useLayoutEffect(() => {
    const svg = svgRef.current
    if (!svg || dados.length === 0) return
    const { width, height } = svg.getBoundingClientRect()
    txRef.current = { x: (width || 600) / 2, y: (height || 500) / 2, k: 1 }
    applyTx()
  }, [dados, applyTx])

  // ── Rebuild SVG DOM ───────────────────────────────────────────────────────
  useEffect(() => {
    const container = gRef.current
    if (!container) return

    const { nodes, edges } = initNodes(dados)

    // preserve positions from previous simulation
    const prevNm = nmRef.current
    for (const n of nodes) {
      const prev = prevNm.get(n.id)
      if (prev) { n.x = prev.x; n.y = prev.y; n.vx = prev.vx; n.vy = prev.vy }
    }

    const visNodes = nodes.filter(n => !hiddenIds.has(n.id))
    const visIds = new Set(visNodes.map(n => n.id))
    const visEdges = edges.filter(e => visIds.has(e.source) && visIds.has(e.target))

    nodesRef.current = visNodes
    edgesRef.current = visEdges
    nmRef.current = new Map(visNodes.map(n => [n.id, n]))
    nodeElRef.current.clear()
    edgeElRef.current.clear()

    container.innerHTML = ''
    container.appendChild(buildArrowDefs())

    const eg = svgEl<SVGGElement>('g')
    for (const e of visEdges) {
      const line = svgEl<SVGLineElement>('line')
      setAttrs(line, { stroke: '#cbd5e1', 'stroke-width': '1.5', 'marker-end': 'url(#dg-arrow)' })
      eg.appendChild(line)
      edgeElRef.current.set(`${e.source}||${e.target}`, line)
    }
    container.appendChild(eg)

    const ng = svgEl<SVGGElement>('g')
    for (const n of visNodes) {
      const g = buildNodeEl(n)
      ng.appendChild(g)
      nodeElRef.current.set(n.id, g)
    }
    container.appendChild(ng)
  }, [dados, hiddenIds])

  // ── Animation loop (starts once, reads refs) ──────────────────────────────
  useEffect(() => {
    const loop = () => {
      applyRepulsion(nodesRef.current)
      applyLinks(edgesRef.current, nmRef.current)
      integrate(nodesRef.current)

      for (const n of nodesRef.current) {
        const g = nodeElRef.current.get(n.id)
        if (g) g.setAttribute('transform', `translate(${n.x.toFixed(1)},${n.y.toFixed(1)})`)
      }
      for (const e of edgesRef.current) {
        const line = edgeElRef.current.get(`${e.source}||${e.target}`)
        const s = nmRef.current.get(e.source), t = nmRef.current.get(e.target)
        if (line && s && t) {
          line.setAttribute('x1', s.x.toFixed(1)); line.setAttribute('y1', s.y.toFixed(1))
          line.setAttribute('x2', t.x.toFixed(1)); line.setAttribute('y2', t.y.toFixed(1))
        }
      }
      rafRef.current = requestAnimationFrame(loop)
    }
    rafRef.current = requestAnimationFrame(loop)
    return () => cancelAnimationFrame(rafRef.current)
  }, [])

  // ── Pointer + wheel events (registered once) ──────────────────────────────
  useEffect(() => {
    const svg = svgRef.current
    if (!svg) return

    const nodeIdOf = (target: EventTarget | null): string | null => {
      let el = target as Element | null
      while (el && el !== svg) {
        // SVGElement does not have .dataset — getAttribute is correct here
        const id = el.getAttribute('data-nid') // NOSONAR
        if (id) return id
        el = el.parentElement
      }
      return null
    }

    const toSvgCoords = (cx: number, cy: number) => {
      const r = svg.getBoundingClientRect()
      const { x, y, k } = txRef.current
      return { x: (cx - r.left - x) / k, y: (cy - r.top - y) / k }
    }

    const onDown = (e: PointerEvent) => {
      const nid = nodeIdOf(e.target)
      if (nid) {
        const n = nmRef.current.get(nid)
        if (!n) return
        const p = toSvgCoords(e.clientX, e.clientY)
        dragRef.current = { id: nid, ox: p.x - n.x, oy: p.y - n.y }
        n.fx = n.x; n.fy = n.y
        svg.setPointerCapture(e.pointerId)
        e.stopPropagation()
      } else {
        panRef.current = { sx: e.clientX, sy: e.clientY, otx: txRef.current.x, oty: txRef.current.y }
        svg.setPointerCapture(e.pointerId)
      }
    }

    const onMove = (e: PointerEvent) => {
      if (dragRef.current) {
        const { id, ox, oy } = dragRef.current
        const n = nmRef.current.get(id)
        if (!n) return
        const p = toSvgCoords(e.clientX, e.clientY)
        n.fx = p.x - ox; n.fy = p.y - oy
      } else if (panRef.current) {
        const { sx, sy, otx, oty } = panRef.current
        txRef.current = { ...txRef.current, x: otx + (e.clientX - sx), y: oty + (e.clientY - sy) }
        applyTx()
      }
    }

    const onUp = () => {
      if (dragRef.current) {
        const n = nmRef.current.get(dragRef.current.id)
        if (n) { n.fx = null; n.fy = null }
        dragRef.current = null
      }
      panRef.current = null
    }

    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      const r = svg.getBoundingClientRect()
      const mx = e.clientX - r.left, my = e.clientY - r.top
      const { x, y, k } = txRef.current
      const nk = Math.min(Math.max(k * (e.deltaY < 0 ? 1.12 : 0.89), 0.05), 10)
      txRef.current = { x: mx - (mx - x) * (nk / k), y: my - (my - y) * (nk / k), k: nk }
      applyTx()
    }

    svg.addEventListener('pointerdown', onDown)
    svg.addEventListener('pointermove', onMove)
    svg.addEventListener('pointerup', onUp)
    svg.addEventListener('wheel', onWheel, { passive: false })

    return () => {
      svg.removeEventListener('pointerdown', onDown)
      svg.removeEventListener('pointermove', onMove)
      svg.removeEventListener('pointerup', onUp)
      svg.removeEventListener('wheel', onWheel)
    }
  }, [applyTx])

  // ── Zoom buttons ──────────────────────────────────────────────────────────
  const zoomBy = useCallback((factor: number) => {
    const svg = svgRef.current
    if (!svg) return
    const { width, height } = svg.getBoundingClientRect()
    const cx = width / 2, cy = height / 2
    const { x, y, k } = txRef.current
    const nk = Math.min(Math.max(k * factor, 0.05), 10)
    txRef.current = { x: cx - (cx - x) * (nk / k), y: cy - (cy - y) * (nk / k), k: nk }
    applyTx()
  }, [applyTx])

  // ── Legend toggle ─────────────────────────────────────────────────────────
  const toggleHidden = useCallback((id: string) => {
    setHiddenIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }, [])

  const legendItem = (n: SimNode) => (
    <li key={n.id}>
      <button
        type="button"
        className={`dep-graph__legend-btn${hiddenIds.has(n.id) ? ' dep-graph__legend-btn--hidden' : ''}`}
        onClick={() => toggleHidden(n.id)}
        title={n.id}
      >
        <span className={`dep-graph__legend-dot dep-graph__legend-dot--${n.tipo === 'projeto' ? 'projeto' : 'dep'}`} />
        <span className="dep-graph__legend-label">{n.id}</span>
      </button>
    </li>
  )

  return (
    <div className="dep-graph">
      <div className="dep-graph__canvas-wrap">
        <svg ref={svgRef} className="dep-graph__svg">
          <g ref={gRef} />
        </svg>
        <div className="dep-graph__controls">
          <button type="button" className="dep-graph__ctrl-btn" onClick={() => zoomBy(1.3)} aria-label="Zoom in">+</button>
          <button type="button" className="dep-graph__ctrl-btn" onClick={() => zoomBy(0.77)} aria-label="Zoom out">−</button>
        </div>
      </div>
      <aside className="dep-graph__legend">
        {projects.length > 0 && (
          <div className="dep-graph__legend-group">
            <span className="dep-graph__legend-header">{t('dependencies.graph.projects')}</span>
            <ul className="dep-graph__legend-list">{projects.map(legendItem)}</ul>
          </div>
        )}
        {depNodes.length > 0 && (
          <div className="dep-graph__legend-group">
            <span className="dep-graph__legend-header">{t('dependencies.graph.dependencies')}</span>
            <ul className="dep-graph__legend-list">{depNodes.map(legendItem)}</ul>
          </div>
        )}
      </aside>
    </div>
  )
}
