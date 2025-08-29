/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2025 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.core.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.marmotgraph.commons.model.ResultWithExecutionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@AllArgsConstructor
public class MetricsAspect {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * org.marmotgraph..*.*(..)) && !execution(public * org.marmotgraph.commons.*.*(..))) || execution(public * com.arangodb..*.*(..)))")
    public Object executionTime(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long start = System.nanoTime();
        Object result = null;
        try {
            result = proceedingJoinPoint.proceed();
        }
        catch (Throwable e) {
            throw e;
        }
        finally {
            long duration = System.nanoTime()-start;
            if(result instanceof ResultWithExecutionDetails) {
                ((ResultWithExecutionDetails<?>)result).setExecutionDetails(start, System.nanoTime());
            } else if (result instanceof ResponseEntity) {
                Object body = ((ResponseEntity<?>) result).getBody();
                if(body instanceof ResultWithExecutionDetails){
                    ((ResultWithExecutionDetails<?>)body).setExecutionDetails(start, System.nanoTime());
                }
            }
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if(attributes != null) {
                HttpServletResponse response = attributes.getResponse();
                if(response!=null){
                    response.setHeader("Server-Timing", String.format("total;dur=%.2f", duration/1_000_000.0));
                }
            }
        }
        return result;
    }

}
