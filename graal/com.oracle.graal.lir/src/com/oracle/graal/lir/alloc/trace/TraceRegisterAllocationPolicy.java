/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.lir.alloc.trace;

import java.util.ArrayList;

import com.oracle.graal.compiler.common.alloc.RegisterAllocationConfig;
import com.oracle.graal.compiler.common.alloc.Trace;
import com.oracle.graal.compiler.common.alloc.TraceBuilderResult;
import com.oracle.graal.lir.LIR;
import com.oracle.graal.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import com.oracle.graal.lir.gen.LIRGenerationResult;
import com.oracle.graal.lir.gen.LIRGeneratorTool.MoveFactory;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Manages the selection of allocation strategies.
 */
public final class TraceRegisterAllocationPolicy {

    protected abstract class AllocationStrategy {
        TraceAllocationPhase<TraceAllocationContext> allocator;

        public final TraceAllocationPhase<TraceAllocationContext> getAllocator() {
            if (allocator == null) {
                allocator = initAllocator(target, lirGenRes, spillMoveFactory, registerAllocationConfig, cachedStackSlots, resultTraces, neverSpillConstants, strategies);
            }
            return allocator;
        }

        protected final LIR getLIR() {
            return lirGenRes.getLIR();
        }

        protected final TraceBuilderResult getTraceBuilderResult() {
            return resultTraces;
        }

        /**
         * Returns {@code true} if the allocation strategy should be used for {@code trace}.
         */
        public abstract boolean shouldApplyTo(Trace trace);

        @SuppressWarnings("hiding")
        protected abstract TraceAllocationPhase<TraceAllocationContext> initAllocator(TargetDescription target, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory,
                        RegisterAllocationConfig registerAllocationConfig, AllocatableValue[] cachedStackSlots, TraceBuilderResult resultTraces, boolean neverSpillConstant,
                        ArrayList<AllocationStrategy> strategies);
    }

    private final TargetDescription target;
    private final LIRGenerationResult lirGenRes;
    private final MoveFactory spillMoveFactory;
    private final RegisterAllocationConfig registerAllocationConfig;
    private final AllocatableValue[] cachedStackSlots;
    private final TraceBuilderResult resultTraces;
    private final boolean neverSpillConstants;

    private final ArrayList<AllocationStrategy> strategies;

    public TraceRegisterAllocationPolicy(TargetDescription target, LIRGenerationResult lirGenRes, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig,
                    AllocatableValue[] cachedStackSlots, TraceBuilderResult resultTraces, boolean neverSpillConstant) {
        this.target = target;
        this.lirGenRes = lirGenRes;
        this.spillMoveFactory = spillMoveFactory;
        this.registerAllocationConfig = registerAllocationConfig;
        this.cachedStackSlots = cachedStackSlots;
        this.resultTraces = resultTraces;
        this.neverSpillConstants = neverSpillConstant;

        this.strategies = new ArrayList<>(3);
    }

    public void appendStrategy(AllocationStrategy strategy) {
        strategies.add(strategy);
    }

    public TraceAllocationPhase<TraceAllocationContext> selectStrategy(Trace trace) {
        for (AllocationStrategy strategy : strategies) {
            if (strategy.shouldApplyTo(trace)) {
                return strategy.getAllocator();
            }
        }
        throw JVMCIError.shouldNotReachHere("No Allocation Strategy found!");
    }

}
