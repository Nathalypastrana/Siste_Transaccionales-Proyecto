interface LogoHotelProps {
  tamaño?: number;
  mostrarNombre?: boolean;
}

export default function LogoHotel({
  tamaño = 55,
  mostrarNombre = false,
}: LogoHotelProps) {
  return (
    <div className="logo-hotel">
      <img
        src="/img/logo-hotel1.png"
        alt="Logo Hotel En mi pueblito"
        width={tamaño}
        height={tamaño}
        style={{ objectFit: "contain" }}
      />

      {mostrarNombre && <span>Hotel En mi pueblito</span>}
    </div>
  );
}