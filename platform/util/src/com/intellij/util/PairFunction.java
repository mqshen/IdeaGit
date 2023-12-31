/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

/**
 * Deprecated. Please use {@link BiFunction} instead
 * @author max
 */
@FunctionalInterface
@ApiStatus.Obsolete
public interface PairFunction<Arg1, Arg2, ResultType> extends BiFunction<Arg1, Arg2, ResultType> {

  ResultType fun(Arg1 t, Arg2 v);

  @Override
  default ResultType apply(Arg1 t, Arg2 v) {
    return fun(t, v);
  }
}
