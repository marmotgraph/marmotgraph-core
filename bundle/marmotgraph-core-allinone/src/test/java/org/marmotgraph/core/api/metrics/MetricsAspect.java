/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.core.api.metrics;

import org.marmotgraph.commons.AuthContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Aspect
@Component
@ConditionalOnProperty(value = "org.marmotgraph.metrics", havingValue = "true", matchIfMissing = false)
public class MetricsAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    TestInformation testInformation;

    @Autowired
    AuthContext authContext;

    @Around("(execution(public * org.marmotgraph..*.*(..)) && !execution(public * org.marmotgraph.commons.*.*(..))) || execution(public * com.arangodb..*.*(..)))")
    public Object time(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if(testInformation.getRunId()!=null) {
            Instant start = Instant.now();
            Object value;
            try {
                value = proceedingJoinPoint.proceed();
            } catch (Throwable throwable) {
                throw throwable;
            } finally {
                if(authContext!=null && authContext.getAuthTokens()!=null) {
                    Instant end = Instant.now();
                    MethodExecution execution = new MethodExecution();
                    execution.setPackageName(proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName());
                    execution.setMethodName(proceedingJoinPoint.getSignature().getName());
                    execution.setStartTime(start.toEpochMilli());
                    execution.setEndTime(end.toEpochMilli());
                    testInformation.getMethodExecutions().computeIfAbsent(authContext.getAuthTokens().getTransactionId(), f -> new ArrayList<>()).add(execution);
                }
            }
            return value;
        }
        else {
            return proceedingJoinPoint.proceed();
        }
    }
}
