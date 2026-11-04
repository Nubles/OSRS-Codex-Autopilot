package com.codex.autopilot;

import com.codex.autopilot.core.KittenBook;
import com.codex.autopilot.core.RumourBook;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.RuneScapeProfileChanged;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RuneLite has already moved to the new profile when it says the profile changed (ConfigManager
 * sets its key, then posts RuneScapeProfileChanged), as on a hop between a normal world and a
 * Leagues, Deadman or beta world. What the last account had not saved yet belongs to that account,
 * never to the one now in.
 */
public class ProfileSwitchTest
{
	private static final String OLD = "rsprofile.old";
	private static final String NEW = "rsprofile.new";

	@Test
	public void unsavedRumourAndKittenGoToTheProfileTheyCameFrom() throws Exception
	{
		Gson gson = new Gson();
		ConfigManager configManager = Mockito.mock(ConfigManager.class);
		when(configManager.getRSProfileKey()).thenReturn(NEW);
		CodexAutopilotPlugin plugin = new CodexAutopilotPlugin();
		set(plugin, "configManager", configManager);
		set(plugin, "gson", gson);
		set(plugin, "clientThread", Mockito.mock(ClientThread.class));

		RumourBook rumour = RumourBook.empty();
		rumour.read(false, "Your current rumour target is a grey chinchompa. Cervus was the source of the rumour.", 1000);
		set(plugin, "rumour", rumour);
		set(plugin, "rumourDirty", true);
		KittenBook kitten = KittenBook.empty();
		kitten.read("The kitten gobbles up the fish.", 1000);
		set(plugin, "kitten", kitten);
		set(plugin, "kittenDirty", true);

		plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged(OLD, NEW));

		verify(configManager, never()).setRSProfileConfiguration(eq(CodexAutopilotConfig.GROUP), anyString(), any());
		verify(configManager).setConfiguration(eq(CodexAutopilotConfig.GROUP), eq(OLD), eq(RumourBook.KEY), contains("GREY_CHINCHOMPA"));
		verify(configManager).setConfiguration(eq(CodexAutopilotConfig.GROUP), eq(OLD), eq(KittenBook.KEY), contains("KITTEN"));
	}

	private static void set(Object target, String name, Object value) throws ReflectiveOperationException
	{
		Field f = CodexAutopilotPlugin.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}
}
