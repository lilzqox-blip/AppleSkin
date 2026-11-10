package squeek.appleskin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

/**
 * Memindah slot hotbar yang dipilih DAN mengirim paket perubahannya ke server.
 *
 * player.getInventory().setSelectedSlot(slot) saja hanya mengubah tampilan di
 * client -- server tidak tahu item apa yang sebenarnya kamu pegang sampai paket
 * UpdateSelectedSlotC2SPacket dikirim (ini normalnya dikirim otomatis oleh
 * Minecraft saat kamu menekan tombol 1-9 atau scroll, tapi TIDAK terkirim kalau
 * slot diganti lewat kode seperti modul-modul di sini). Tanpa paket ini, efek
 * yang dicek server dari item yang dipegang (misalnya axe men-disable shield,
 * atau charge anchor dari glowstone) tidak akan terjadi walau tampilan sudah
 * berubah di layarmu.
 */
public final class HotbarUtil
{
	private HotbarUtil()
	{
	}

	public static void select(MinecraftClient mc, int slot)
	{
		if (mc.player == null || mc.getNetworkHandler() == null)
			return;
		if (slot < 0 || slot > 8)
			return;
		if (mc.player.getInventory().getSelectedSlot() == slot)
			return;

		mc.player.getInventory().setSelectedSlot(slot);
		mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
	}
}
