package com.codex.autopilot;

import com.codex.autopilot.core.PortTask;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.DBTableID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Port tasks read from the game's own tables, laid out as RuneLite's gameval DBTableID describes
 * them: the port task table's columns (task id, name, level, starting, cargo and ending port, cargo as
 * (item, amount), bounty creature, part and count) and the docking point table's nice name. The row
 * values are the wiki's (Courier tasks: Port Sarim to The Pandemonium, Platebodies, 1; Bounty tasks:
 * Catherby, Albatross, beak, 5).
 */
public class PortCatalogTest
{
	static final int SARIM = DBTableID.SailingDock.Row.SAILING_DOCK_PORT_SARIM;
	static final int PANDEMONIUM = DBTableID.SailingDock.Row.SAILING_DOCK_THE_PANDEMONIUM;
	static final int CATHERBY = DBTableID.SailingDock.Row.SAILING_DOCK_CATHERBY;
	static final int COURIER_ROW = DBTableID.PortTask.Row.PORT_TASK_PORT_SARIM_COURIER_0;
	static final int BOUNTY_ROW = 90001;
	static final int QUEST_ROW = 90002;

	static Object[] v(Object... values)
	{
		return values;
	}

	static Client client()
	{
		Client c = mock(Client.class);
		when(c.getDBTableField(anyInt(), anyInt(), anyInt())).thenReturn(new Object[0]);
		when(c.getDBTableRows(DBTableID.PortTask.ID)).thenReturn(Arrays.asList(COURIER_ROW, BOUNTY_ROW, QUEST_ROW));
		// Port Sarim's platebodies.
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_TASK_ID, 0)).thenReturn(v(1));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_NAME, 0)).thenReturn(v("Platebody delivery"));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_LEVEL_REQUIRED, 0)).thenReturn(v(1));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_STARTING_PORT, 0)).thenReturn(v(SARIM));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_CARGO_PORT, 0)).thenReturn(v(SARIM));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_ENDING_PORT, 0)).thenReturn(v(PANDEMONIUM));
		when(c.getDBTableField(COURIER_ROW, DBTableID.PortTask.COL_CARGO, 0)).thenReturn(v(700, 1));
		// Catherby's albatross beaks.
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_TASK_ID, 0)).thenReturn(v(2));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_LEVEL_REQUIRED, 0)).thenReturn(v(50));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_STARTING_PORT, 0)).thenReturn(v(CATHERBY));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_CARGO_PORT, 0)).thenReturn(v(CATHERBY));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_ENDING_PORT, 0)).thenReturn(v(CATHERBY));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_BOUNTY_TARGET_ALIVE, 0)).thenReturn(v(800));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_BOUNTY_OBJECT, 0)).thenReturn(v(701));
		when(c.getDBTableField(BOUNTY_ROW, DBTableID.PortTask.COL_BOUNTY_OBJECT_AMOUNT, 0)).thenReturn(v(5));
		// A quest's own task: no ports to speak of.
		when(c.getDBTableField(QUEST_ROW, DBTableID.PortTask.COL_TASK_ID, 0)).thenReturn(v(3));
		when(c.getDBTableField(SARIM, DBTableID.SailingDock.COL_NICE_NAME, 0)).thenReturn(v("Port Sarim"));
		when(c.getDBTableField(PANDEMONIUM, DBTableID.SailingDock.COL_NICE_NAME, 0)).thenReturn(v("The Pandemonium"));
		when(c.getDBTableField(CATHERBY, DBTableID.SailingDock.COL_NICE_NAME, 0)).thenReturn(v("Catherby"));
		ItemComposition crate = mock(ItemComposition.class);
		when(crate.getName()).thenReturn("Crate of platebodies");
		when(c.getItemDefinition(700)).thenReturn(crate);
		ItemComposition beak = mock(ItemComposition.class);
		when(beak.getName()).thenReturn("Albatross beak");
		when(c.getItemDefinition(701)).thenReturn(beak);
		NPCComposition bird = mock(NPCComposition.class);
		when(bird.getName()).thenReturn("Albatross");
		when(c.getNpcDefinition(800)).thenReturn(bird);
		return c;
	}

	@Test
	public void aCourierTaskIsReadFromItsRow()
	{
		PortTask t = new PortCatalog(client()).lookup(1);
		assertTrue(t.isCourier());
		assertEquals("Platebody delivery", t.getName());
		assertEquals(1, t.getLevel());
		assertEquals("Port Sarim", t.getBoard());
		assertEquals("Port Sarim", t.getFrom());
		assertEquals(SARIM, t.getFromDock());
		assertEquals("The Pandemonium", t.getTo());
		assertEquals(PANDEMONIUM, t.getToDock());
		assertEquals("Platebodies", t.getCargo());
		assertEquals(700, t.getCargoItem());
		assertEquals(1, t.getAmount());
	}

	@Test
	public void aBountyIsReadFromItsRow()
	{
		PortTask t = new PortCatalog(client()).lookup(2);
		assertEquals(PortTask.Type.BOUNTY, t.getType());
		assertEquals("Albatross", t.getMonster());
		assertEquals("Albatross beak", t.getItem());
		assertEquals(701, t.getItemId());
		assertEquals(5, t.getNeed());
		assertEquals(50, t.getLevel());
		assertEquals("Catherby", t.getBoard());
	}

	@Test
	public void rowsItCannotReadGiveNothing()
	{
		PortCatalog catalog = new PortCatalog(client());
		assertNull(catalog.lookup(3));
		assertNull(catalog.lookup(42));
		// A cache that is not ready yet gives nothing, and is asked again later.
		Client empty = mock(Client.class);
		when(empty.getDBTableRows(anyInt())).thenThrow(new IllegalStateException("not loaded"));
		PortCatalog none = new PortCatalog(empty);
		assertNull(none.lookup(1));
		org.mockito.Mockito.doReturn(java.util.Collections.emptyList()).when(empty).getDBTableRows(anyInt());
		assertNull(none.lookup(1));
	}

	@Test
	public void theCrateIsSaidOnce()
	{
		assertEquals("Platebodies", PortCatalog.cargoName("Crate of platebodies"));
		assertEquals("Dye", PortCatalog.cargoName("Crates of dye"));
		assertEquals("Spices", PortCatalog.cargoName("Spices"));
		assertNull(PortCatalog.cargoName(null));
	}
}
