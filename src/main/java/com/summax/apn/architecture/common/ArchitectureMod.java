/*
 * MIT License
 *
 * Copyright (c) 2017 Benjamin K
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.summax.apn.architecture.common;

import com.summax.apn.architecture.client.proxy.ClientProxy;
import com.summax.apn.architecture.common.proxy.CommonProxy;
import com.summax.apn.APortNeeded;
import com.summax.apn.architecture.common.compat.HeldShapeMaterial;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.DistExecutor;


/**
 * Architecture module: shapes, sawbench and tools.
 */
public class ArchitectureMod {
    public static final String MOD_ID = APortNeeded.MOD_ID;

    public static final ArchitectureContent CONTENT = new ArchitectureContent();
    public static ArchitectureMod INSTANCE;

    public static CommonProxy PROXY;

    public ArchitectureMod(IEventBus modEventBus) {
        ArchitectureMod.INSTANCE = this;
        // DistExecutor keeps the client proxy, and the client classes it references, from loading on a dedicated server.
        PROXY = DistExecutor.unsafeRunForDist(() -> () -> new ClientProxy(), () -> () -> new CommonProxy());

        modEventBus.addListener(this::onSetup);
        modEventBus.register(CONTENT);
        modEventBus.register(PROXY);
        MinecraftForge.EVENT_BUS.addListener(HeldShapeMaterial::onEntityPlace);
    }

    public void onSetup(FMLCommonSetupEvent e) {
        PROXY.setup(e);
    }

}

