package com.codex.autopilot.core;

import com.google.gson.Gson;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The pet kitten as Autopilot reads and plans it. The facts are pinned to their sources: the OSRS
 * wiki (Kitten: hunger 24 + 3 + 3 minutes, attention 25 / 18 / 25 / 51 + 7 + 7 minutes, growth 120
 * x 90 seconds; Cat: overgrown after another five to six hours, no needs; Transcript:Kitten and
 * Transcript:Cat for every line read) and RuneLite gameval (NpcID.KITTENPET*, GROWNCAT*,
 * OVERGROWNCAT*; ItemID.KITTENOBJECT*).
 */
public class KittenTest
{
	static final Gson GSON = new Gson();
	static final long NOW = AutopilotCoreTest.T0;
	static final long MS = NOW * 1000;
	static final int KITTEN = NpcID.KITTENPET_BROWN;
	static final int CAT = NpcID.GROWNCAT_BROWN;

	// ---- The facts ----

	@Test
	public void idsAreRuneLites()
	{
		assertEquals(5591, NpcID.KITTENPET1);
		assertEquals(5597, NpcID.KITTENPET_HELL);
		assertEquals(1619, NpcID.GROWNCAT);
		assertEquals(1625, NpcID.GROWNCAT_HELL);
		assertEquals(5598, NpcID.OVERGROWNCAT);
		assertEquals(5604, NpcID.OVERGROWNCAT_HELL);
		assertEquals(Kitten.Stage.KITTEN, Kitten.stage(NpcID.KITTENPET_BLUEGREY));
		assertEquals(Kitten.Stage.KITTEN, Kitten.stage(NpcID.KITTENPET_HELL));
		assertEquals(Kitten.Stage.CAT, Kitten.stage(NpcID.GROWNCAT_LIGHT));
		assertEquals(Kitten.Stage.OVERGROWN, Kitten.stage(NpcID.OVERGROWNCAT_BLACK));
		// Lazy and wily cats, and other people's pets, are none of this.
		assertNull(Kitten.stage(NpcID.LAZYCAT));
		assertNull(Kitten.stage(NpcID.WILEYCAT));
		assertNull(Kitten.stage(NpcID.LOSTKITTEN));
		assertTrue(Kitten.isKittenItem(ItemID.KITTENOBJECT));
		assertTrue(Kitten.isKittenItem(ItemID.KITTENOBJECT_BLUEGREY));
		assertTrue(Kitten.isKittenItem(ItemID.KITTENOBJECT_HELL));
		assertFalse(Kitten.isKittenItem(ItemID.GROWNCATOBJECT));
		// The kitten's own colour pictures the step.
		assertEquals(ItemID.KITTENOBJECT_BROWN, Kitten.item(NpcID.KITTENPET_BROWN));
		assertEquals(ItemID.KITTENOBJECT_HELL, Kitten.item(NpcID.GROWNCAT_HELL));
	}

	@Test
	public void timesAreTheWikis()
	{
		assertEquals(24, Kitten.HUNGER_WARN_MINUTES);
		assertEquals(3, Kitten.HUNGER_STEP_MINUTES);
		assertEquals(25, Kitten.Care.NEW.getMinutes());
		assertEquals(18, Kitten.Care.STROKE.getMinutes());
		assertEquals(25, Kitten.Care.STROKES.getMinutes());
		assertEquals(51, Kitten.Care.WOOL.getMinutes());
		assertEquals(7, Kitten.LONELY_STEP_MINUTES);
		// 120 growth ticks of 150 game ticks (90 seconds): three hours.
		assertEquals(3 * 3600, Kitten.GROW_SECONDS);
		assertEquals(5, Kitten.OVERGROWN_MIN_HOURS);
		assertEquals(6, Kitten.OVERGROWN_MAX_HOURS);
	}

	// ---- Reading the game ----

	/** A kitten seen following, fed and stroked once at the start of the clock. */
	static KittenBook fedAndStroked()
	{
		KittenBook b = KittenBook.empty();
		b.look(3, KITTEN, false, MS);
		assertTrue(b.read("The kitten gobbles up the fish.", MS));
		assertTrue(b.read("You softly stroke your cat.", MS));
		return b;
	}

	@Test
	public void aKittenFirstSeenMidLifeHasNoTimesUntilTheGameGivesOne()
	{
		KittenBook b = KittenBook.empty();
		assertTrue(b.look(3, KITTEN, false, MS));
		assertEquals(Kitten.Stage.KITTEN, b.getStage());
		assertTrue(b.isOut());
		assertEquals(-1, b.hungerLeft());
		assertEquals(-1, b.lonelyLeft());
		assertFalse(b.isAgeKnown());
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
		// The game says it is hungry: six minutes until it runs off (3 to the second warning, 3 more).
		assertTrue(b.read("<col=ef1020>Your kitten is hungry.</col>", MS));
		assertEquals(6 * 60, b.hungerLeft());
		List<Candidate> c = Kittens.candidates(b, NOW);
		assertEquals(1, c.size());
		assertEquals(Kittens.FEED, c.get(0).getKey());
		assertEquals(Tier.WASTING, c.get(0).getTier());
		assertEquals(NOW + 360, c.get(0).getDeadline());
		assertEquals("It is hungry: it runs off in about 6 min unless fed", c.get(0).getWhy());
		assertEquals(Kitten.FOOD, c.get(0).getBring());
		assertEquals(Candidate.Kind.KITTEN, c.get(0).getKind());
		assertEquals(ItemID.KITTENOBJECT_BROWN, c.get(0).getItemId());
		// Very hungry: three minutes.
		b.look(180, KITTEN, false, MS);
		assertTrue(b.read("Your kitten is very hungry.", MS));
		assertEquals(3 * 60, b.hungerLeft());
		assertEquals("It is very hungry: it runs off in about 3 min unless fed", Kittens.candidates(b, NOW).get(0).getWhy());
		// Fed: content again.
		assertTrue(b.read("The kitten laps up the milk.", MS));
		assertEquals(0, b.getHungry());
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
	}

	@Test
	public void aMealIsOfferedFiveMinutesBeforeTheWarningAndGoesToWasteWhenItIsDue()
	{
		KittenBook b = fedAndStroked();
		// The wiki: a meal lasts 24 minutes to the warning, and 6 more to running off.
		assertEquals(30 * 60, b.hungerLeft());
		b.look(12 * 60, KITTEN, false, MS);
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
		// At 13 minutes one stroke (18 minutes) is five from asking again: a ready step, not yet urgent.
		b.look(60, KITTEN, false, MS);
		assertEquals(Kittens.STROKE, Kittens.candidates(b, NOW).get(0).getKey());
		assertEquals(Tier.READY, Kittens.candidates(b, NOW).get(0).getTier());
		assertEquals(0, Kittens.candidates(b, NOW).get(0).getDeadline());
		// At 19 the meal is five from its warning and the stroke is past due: both, as one step.
		b.look(6 * 60, KITTEN, false, MS);
		List<Candidate> c = Kittens.candidates(b, NOW);
		assertEquals(Kittens.CARE, c.get(0).getKey());
		assertEquals("Feed and stroke your kitten", c.get(0).getTitle());
		assertEquals(Tier.WASTING, c.get(0).getTier());
		assertTrue(c.get(0).getWhy(), c.get(0).getWhy().startsWith("Fed 19 min ago; it asks for food after 24 min (the wiki's time)"));
		assertTrue(c.get(0).getWhy(), c.get(0).getWhy().contains("Last stroked 19 min ago; one stroke lasts 18 min (the wiki's time)"));
	}

	@Test
	public void strokingTwiceBuysTwentyFiveMinutesAndWoolFiftyOne()
	{
		KittenBook b = fedAndStroked();
		assertEquals(Kitten.Care.STROKE, b.getCare());
		b.look(20, KITTEN, false, MS);
		b.read("You softly stroke your cat.", MS);
		assertEquals(Kitten.Care.STROKES, b.getCare());
		assertEquals((25 + 14) * 60, b.lonelyLeft());
		// A stroke much later is one stroke again.
		b.look(600, KITTEN, false, MS);
		b.read("You softly stroke your cat.", MS);
		assertEquals(Kitten.Care.STROKE, b.getCare());
		// The player's own line when it plays with the wool (from the chat's dialogue).
		b.read("Zezima|That kitten loves to play with that ball of wool. I think it is its favourite.", MS);
		assertEquals(Kitten.Care.WOOL, b.getCare());
		assertEquals((51 + 14) * 60, b.lonelyLeft());
	}

	@Test
	public void attentionWarningsCountDownSevenAndFourteenMinutes()
	{
		KittenBook b = fedAndStroked();
		b.look(18 * 60, KITTEN, false, MS);
		b.read("Your kitten wants attention.", MS);
		assertEquals(14 * 60, b.lonelyLeft());
		b.read("The kitten gobbles up the fish.", MS);
		List<Candidate> c = Kittens.candidates(b, NOW);
		assertEquals(Kittens.STROKE, c.get(0).getKey());
		assertEquals("It wants attention: it runs off in about 14 min unless stroked", c.get(0).getWhy());
		assertEquals(Kittens.STROKE_TIP, c.get(0).getBring());
		b.look(7 * 60, KITTEN, false, MS);
		b.read("Your kitten really wants attention.", MS);
		assertEquals(7 * 60, b.lonelyLeft());
		assertEquals("It feels lonely: it runs off in about 7 min unless stroked", Kittens.candidates(b, NOW).get(0).getWhy());
	}

	@Test
	public void itsOwnTimesAreLearnedFromTheGamesWarnings()
	{
		KittenBook b = fedAndStroked();
		for (int i = 0; i < KittenBook.TRUST; i++)
		{
			// This kitten asks after 22.5 minutes, as the game's 90 second ticks fall.
			b.look(22 * 60 + 30, KITTEN, false, MS);
			assertFalse(b.isLearned(KittenBook.FED));
			b.read("Your kitten is hungry.", MS);
			b.read("The kitten gobbles up the fish.", MS);
			b.read("You softly stroke your cat.", MS);
		}
		assertTrue(b.isLearned(KittenBook.FED));
		assertEquals(22.5, b.hungerMinutes(), 1e-9);
		assertEquals((long) (22.5 * 60) + 6 * 60, b.hungerLeft());
		b.look(18 * 60, KITTEN, false, MS);
		assertTrue(Kittens.candidates(b, NOW).get(0).getWhy().contains("it asks for food after 22.5 min (your kitten's own time)"));
		// A meal missed in the chat would measure double: not a measure of anything, so it is not learned.
		KittenBook c = fedAndStroked();
		c.look(55 * 60, KITTEN, false, MS);
		c.read("Your kitten is hungry.", MS);
		assertEquals(0, c.heard(KittenBook.FED));
	}

	@Test
	public void timeCountsOnlyWhileItFollows()
	{
		KittenBook b = fedAndStroked();
		long left = b.hungerLeft();
		// Picked up: it waits, and no step nags while it is carried.
		assertTrue(b.look(3, -1, true, MS));
		assertFalse(b.isOut());
		assertTrue(b.isHeld());
		b.look(3600, -1, true, MS);
		assertEquals(left, b.hungerLeft());
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
		// Banked: the same.
		b.look(3, -1, false, MS);
		assertFalse(b.isHeld());
		// Out again: the clock runs on from where it stopped.
		b.look(60, KITTEN, false, MS);
		assertEquals(left - 60, b.hungerLeft());
		// Logging out pauses it too.
		assertTrue(b.pause());
		assertFalse(b.isOut());
	}

	@Test
	public void aKittenThatVanishesAsItsTimeRunsOutRanOff()
	{
		KittenBook b = fedAndStroked();
		b.read("Your kitten is hungry.", MS);
		b.look(5 * 60, KITTEN, false, MS);
		b.read("Your kitten is very hungry.", MS);
		b.look(3 * 60, KITTEN, false, MS);
		// One look without it is a teleport's blink; the second, with its time up, means it left.
		assertFalse(b.look(3, -1, false, MS));
		assertTrue(b.look(3, -1, false, MS + 3000));
		assertEquals(KittenBook.GONE_RAN_OFF, b.getGone());
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
		// Gertrude's next kitten is a fresh one: fed, and content for 25 minutes.
		b.look(3, NpcID.KITTENPET_LIGHT, false, MS + 60_000);
		assertNull(b.getGone());
		assertEquals(Kitten.Care.NEW, b.getCare());
		assertEquals(30 * 60 - 3, b.hungerLeft());
		assertEquals((25 + 14) * 60 - 3, b.lonelyLeft());
		assertTrue(b.isAgeKnown());
	}

	@Test
	public void aKittenPutAwayInTimeIsOnlyWaiting()
	{
		KittenBook b = fedAndStroked();
		b.look(3, -1, false, MS);
		b.look(3, -1, false, MS);
		assertNull(b.getGone());
		assertFalse(b.isOut());
	}

	@Test
	public void shooedOrPassedAwayIsGone()
	{
		KittenBook b = fedAndStroked();
		assertTrue(b.read("The cat has run away.", MS));
		assertEquals(KittenBook.GONE_SHOOED, b.getGone());
		KittenBook c = fedAndStroked();
		c.read("Your cat doesn't seem to be in the mood for a snack.", MS);
		assertTrue(c.read("Your cat has passed away.", MS));
		assertEquals(KittenBook.GONE_PASSED, c.getGone());
	}

	@Test
	public void growingIsReadFromTheFollowerAndTheGamesLine()
	{
		KittenBook b = fedAndStroked();
		b.look(3600, KITTEN, false, MS);
		assertEquals(3 * 3600 - 3603, b.growLeft());
		// It turns into a cat while following: no needs any more.
		assertTrue(b.look(3, CAT, false, MS + 1000));
		assertEquals(Kitten.Stage.CAT, b.getStage());
		assertEquals(MS + 1000, b.getGrownAt());
		assertEquals(-1, b.hungerLeft());
		assertEquals(-1, b.lonelyLeft());
		assertTrue(Kittens.candidates(b, NOW).isEmpty());
		// A cat stroked is not a kitten cared for.
		assertFalse(b.read("You softly stroke your cat.", MS));
		b.look(3600, CAT, false, MS);
		assertEquals(3603, b.getCatSeconds());
		assertTrue(b.read("Your cat has grown into a mighty feline, but it will no longer be able to chase vermin.", MS));
		assertEquals(Kitten.Stage.OVERGROWN, b.getStage());

		// The line alone does it too, when the follower was not seen to change.
		KittenBook c = fedAndStroked();
		assertTrue(c.read("Your kitten has grown into a healthy cat that can hunt for itself.", MS + 5));
		assertEquals(Kitten.Stage.CAT, c.getStage());
		assertEquals(MS + 5, c.getGrownAt());
		assertEquals(0, c.getCatSeconds());
	}

	@Test
	public void guessingItsAgeSetsTheGrowthClock()
	{
		KittenBook b = KittenBook.empty();
		b.look(3, KITTEN, false, MS);
		assertFalse(b.isAgeKnown());
		// The wiki's lines, as one message box.
		assertTrue(b.read("After taking a good look at your kitten you guess that its age is: 1 hours 20 minutes. "
			+ "And approximate time until fully adult: 1 hour 40 minutes, assuming you look after it.", MS));
		assertTrue(b.isAgeKnown());
		assertEquals(100 * 60, b.growLeft());
		// Only the age, when the second part comes on its own line.
		KittenBook c = KittenBook.empty();
		c.look(3, KITTEN, false, MS);
		c.read("After taking a good look at your kitten you guess that its age is: 2 hours 30 minutes.", MS);
		assertEquals(30 * 60, c.growLeft());
		c.read("And approximate time until fully adult: 0 hours 29 minutes, assuming you look after it.", MS);
		assertEquals(29 * 60, c.growLeft());
	}

	@Test
	public void otherFollowersAndLinesAreLeftAlone()
	{
		KittenBook b = KittenBook.empty();
		assertFalse(b.look(3, NpcID.LAZYCAT, false, MS));
		assertTrue(b.isBlank());
		assertFalse(b.read("You softly stroke your cat.", MS));
		assertFalse(b.read("Your current rumour target is a grey chinchompa.", MS));
		assertTrue(b.isBlank());
	}

	@Test
	public void savedAndReadBackWithEverythingItLearned()
	{
		KittenBook b = fedAndStroked();
		for (int i = 0; i < 3; i++)
		{
			b.look(23 * 60, KITTEN, false, MS);
			b.read("Your kitten is hungry.", MS);
			b.read("The kitten gobbles up the fish.", MS);
		}
		b.read("Zezima|That kitten loves to play with that ball of wool. I think it is its favourite.", MS);
		b.look(60, KITTEN, false, MS);
		KittenBook back = KittenBook.parse(GSON, b.toJson(GSON));
		assertEquals(b.toJson(GSON), back.toJson(GSON));
		assertEquals(b.hungerLeft(), back.hungerLeft());
		assertEquals(b.lonelyLeft(), back.lonelyLeft());
		assertEquals(23.0, back.hungerMinutes(), 1e-9);
		assertEquals(Kitten.Care.WOOL, back.getCare());
		// Nothing saved, something else, or a later version: a blank book, never an error.
		assertTrue(KittenBook.parse(GSON, null).isBlank());
		assertTrue(KittenBook.parse(GSON, "not json").isBlank());
		assertTrue(KittenBook.parse(GSON, "{\"version\":2,\"stage\":\"CAT\"}").isBlank());
	}

	@Test
	public void theAdvisorRanksAKittenAboutToRunOffFirst()
	{
		KittenBook b = fedAndStroked();
		b.read("Your kitten is hungry.", MS);
		Advisor.Inputs in = new Advisor.Inputs();
		in.kitten = b;
		Settings s = Settings.defaults();
		List<Candidate> all = Advisor.candidates(in, s, NOW);
		assertEquals(Kittens.FEED, all.get(0).getKey());
		Candidate birds = new Candidate("birdhouse", Candidate.Kind.BIRDHOUSE_RUN, Tier.READY, "Birdhouse run", "4 ready", null, null,
			6, 4.8, 0, 4, -1);
		all.add(birds);
		assertEquals(Kittens.FEED, Ranker.rank(all, s, null, NOW).top().getKey());
		// Switched off: no kitten.
		assertTrue(Advisor.candidates(in, Settings.defaults().kitten(false), NOW).isEmpty());
	}
}
