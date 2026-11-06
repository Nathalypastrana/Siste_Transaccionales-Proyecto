interface Props {
  titulo: string
  mensaje: string
  textoConfirmar: string
  onConfirmar: () => void
  onCancelar: () => void
}

/** Diálogo de confirmación para acciones que no se pueden deshacer. */
export default function Confirmar({ titulo, mensaje, textoConfirmar, onConfirmar, onCancelar }: Props) {
  return (
    <div className="modal-fondo" onClick={onCancelar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="modal-titulo">{titulo}</h2>
        <p>{mensaje}</p>
        <div className="modal-acciones">
          <button className="boton-secundario" onClick={onCancelar}>
            Volver
          </button>
          <button className="boton-peligro-solido" onClick={onConfirmar} autoFocus>
            {textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  )
}
