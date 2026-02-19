package com.beet.backend.modules.${moduleName}.infrastructure.output.adapter;

import com.beet.backend.modules.${moduleName}.domain.spi.${Target}Gateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ${Target}GatewayAdapter implements ${Target}Gateway{

// Inject external ports here
// private final ExternalServicePort externalService;

// @Override
// public int getMaxLimit(UUID userId) {
// return externalService.getLimit(userId);
// }
}
