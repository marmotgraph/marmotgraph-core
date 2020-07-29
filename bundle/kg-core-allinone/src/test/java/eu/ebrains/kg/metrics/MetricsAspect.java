/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Aspect
@Component
public class MetricsAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    TestInformation testInformation;


    @Around("(execution(public * eu.ebrains.kg..*.*(..)) && !execution(public * eu.ebrains.kg.commons.*.*(..))) || execution(public * com.arangodb..*.*(..)))")
    public Object time(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Instant start = Instant.now();
        Object value;
        try {
            value = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            Instant end = Instant.now();
            long ms = Duration.between(start, end).toMillis();
            if(ms>1){
            logger.trace(
                    "{},{},{}.{},{},{},{}",
                    testInformation.getRunId(),
                    testInformation.getExecutionNumber(),
                    proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName(),
                    proceedingJoinPoint.getSignature().getName(),
                    ms,
                    start.toEpochMilli(), end.toEpochMilli());
            }
        }
        return value;
    }
}
