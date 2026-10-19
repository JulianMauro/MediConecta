package com.mediconecta.modulos.pago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.repository.PagoRepository;
import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;
import com.mediconecta.modulos.strike.repository.StrikeRepository;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.testutil.Datos;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

	@Mock
	private PagoRepository pagoRepository;
	@Mock
	private StrikeRepository strikeRepository;
	@Mock
	private SuscripcionUsuarioRepository suscripcionUsuarioRepository;

	private PagoService pagoService;
	private Usuario cliente;

	@BeforeEach
	void setUp() {
		pagoService = new PagoService(pagoRepository, strikeRepository, suscripcionUsuarioRepository);
		cliente = Datos.cliente(10L);
	}

	/** Si no, pagar un cobro que soporte anulo saldaria un strike que ya no existe. */
	@Test
	void unPagoAnuladoNoSePuedeConfirmar() {
		Pago pago = Datos.conId(new Pago(cliente, null, null, ConceptoPago.TIEMPO_EXTRA, BigDecimal.TEN), 3L);
		pago.anular();
		when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

		assertThatThrownBy(() -> pagoService.confirmar(cliente, 3L, "REF"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("anulado");
		assertThat(pago.getEstado()).isEqualTo(EstadoPago.ANULADO);
		verify(strikeRepository, never()).findByPagoId(anyLong());
	}

	@Test
	void pagarElTiempoExtraSaldaElStrike() {
		Viaje viaje = Datos.viajeEnCurso(7L, cliente, 45);
		Pago pago = Datos.conId(new Pago(cliente, viaje, null, ConceptoPago.TIEMPO_EXTRA, BigDecimal.TEN), 3L);
		Strike strike = new Strike(cliente, viaje);
		strike.asociarPago(pago);
		when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));
		when(strikeRepository.findByPagoId(3L)).thenReturn(Optional.of(strike));

		pagoService.confirmar(cliente, 3L, "REF");

		assertThat(pago.getEstado()).isEqualTo(EstadoPago.PAGADO);
		assertThat(strike.getEstado()).isEqualTo(EstadoStrike.SALDADO);
	}

	@Test
	void unPagoYaPagadoNoSeAnula() {
		Pago pago = new Pago(cliente, null, null, ConceptoPago.TIEMPO_EXTRA, BigDecimal.TEN);
		pago.confirmar("REF");

		assertThatThrownBy(pago::anular).isInstanceOf(IllegalStateException.class);
		assertThat(pago.getEstado()).isEqualTo(EstadoPago.PAGADO);
	}
}
