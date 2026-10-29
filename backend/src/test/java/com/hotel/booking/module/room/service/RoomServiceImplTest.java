package com.hotel.booking.module.room.service;

import com.hotel.booking.common.BusinessException;
import com.hotel.booking.module.room.mapper.RoomInventoryMapper;
import com.hotel.booking.module.room.mapper.RoomMapper;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomTypeMapper roomTypeMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RoomInventoryMapper inventoryMapper;

    @Test
    void availableInRangeReturnsMapperValue() {
        when(inventoryMapper.availableInRange(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 2L))
                .thenReturn(4);
        RoomServiceImpl service = new RoomServiceImpl(roomTypeMapper, roomMapper, inventoryMapper);

        int available = service.availableInRange(1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), 2L);

        assertThat(available).isEqualTo(4);
    }

    @Test
    void deductInventoryUpdatesEveryNight() {
        LocalDate in = LocalDate.of(2026, 10, 1);
        LocalDate out = LocalDate.of(2026, 10, 3);
        when(inventoryMapper.deductStock(1L, in, 2)).thenReturn(1);
        when(inventoryMapper.deductStock(1L, in.plusDays(1), 2)).thenReturn(1);
        RoomServiceImpl service = new RoomServiceImpl(roomTypeMapper, roomMapper, inventoryMapper);

        assertThat(service.deductInventory(1L, in, out, 2)).isTrue();

        verify(inventoryMapper).deductStock(1L, in, 2);
        verify(inventoryMapper).deductStock(1L, in.plusDays(1), 2);
    }

    @Test
    void deductInventoryThrowsWhenAnyNightIsSoldOut() {
        LocalDate in = LocalDate.of(2026, 10, 1);
        LocalDate out = LocalDate.of(2026, 10, 3);
        when(inventoryMapper.deductStock(1L, in, 1)).thenReturn(1);
        when(inventoryMapper.deductStock(1L, in.plusDays(1), 1)).thenReturn(0);
        RoomServiceImpl service = new RoomServiceImpl(roomTypeMapper, roomMapper, inventoryMapper);

        assertThatThrownBy(() -> service.deductInventory(1L, in, out, 1))
                .isInstanceOf(BusinessException.class);
    }
}
