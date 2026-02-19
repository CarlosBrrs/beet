package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.unit.domain.model.UnitType;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate.UnitConversionProjection;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class UnitConversionRowMapper implements RowMapper<UnitConversionProjection> {

    @Override
    public UnitConversionProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UnitConversionProjection(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("abbreviation"),
                UnitType.valueOf(rs.getString("type")),
                rs.getBoolean("is_base"),
                rs.getBigDecimal("factor_to_base"));
    }
}
