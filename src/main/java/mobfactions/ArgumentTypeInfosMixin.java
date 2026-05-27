package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.Registry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;

import mobfactions.FactionArgument;

import com.mojang.brigadier.arguments.ArgumentType;

@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentTypeInfosMixin {
	@Invoker("register")
	static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> register(Registry<ArgumentTypeInfo<?, ?>> registry, String id, Class<? extends A> argumentClass, ArgumentTypeInfo<A, T> argumentInfo) {
		throw new AssertionError();
	}

	@Inject(method = "bootstrap", at = @At("HEAD"))
	private static void bootstrap(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> callback) {
		register(registry, "faction", FactionArgument.class, SingletonArgumentInfo.contextFree(FactionArgument::faction));
	}
}
