package com.dasugames.bottle;

import java.util.Random;
/**
 * This class contains all of the scenarios that the user can experience.
 * In my defense, I have gotten better since then.
 * @author darren.sue
 *
 */
public class LanguageContent {
	private SingleStage[] allContent;
	// The two values below should always be set to something else.
	String currentStageWords = "error";
	String currentConclusionWords = "error";
	Boolean isResponse = false;
	private final static String DRAWABLE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private String remainingLetters;
	
	/**
	 * This method is hard coded horribleness because I wrote it years ago before I knew what I was doing.
	 * It initializes the language content object with all of the scenarios that the application can
	 * present to the user.
	 */
	public LanguageContent() {
		String[] positiveResponses = new String[] { "Don't give up.",
				"You'll make it.", "Keep moving forwards.", "Keep going.",
				"Breathe.", "Count to three.", "Don't worry.",
				"Everything will be all right.", "Hang in there.",
				"Take it step by step.", "...", "You can do it.",
				"Don't let it defeat you.", "Life goes on.",
				"You'll have a day." };
		allContent = new SingleStage[] {
				new SingleStage("I don't want to do this anymore.",
						positiveResponses),
				new SingleStage("I don't feel like fighting anymore.",
						positiveResponses),
				new SingleStage("I want to sleep and never wake up.",
						positiveResponses),
				new SingleStage("I give up.", positiveResponses),
				new SingleStage("Save me.", positiveResponses),
				new SingleStage("Help me.", positiveResponses),
				new SingleStage("Hate.", positiveResponses),
				new SingleStage("Nobody likes me.", positiveResponses),
				new SingleStage("Nobody loves me.", positiveResponses),
				new SingleStage("No one will care if I am not here.",
						positiveResponses),
				new SingleStage("I don't want to do this anymore.",
						positiveResponses),
				new SingleStage("Why bother.", positiveResponses),
				new SingleStage("I don't care.", positiveResponses),
				new SingleStage("I'll never be happy.", positiveResponses),
				new SingleStage("I feel bad.", positiveResponses),
				new SingleStage("I feel terrible.", positiveResponses),
				new SingleStage("Life is too difficult.", positiveResponses),
				new SingleStage("Can't.", positiveResponses),
				new SingleStage("No.", positiveResponses),
				new SingleStage("I hate you.", positiveResponses),
				new SingleStage("Go away.", positiveResponses),
				new SingleStage("I'll never be successful.", positiveResponses),
				new SingleStage("I am a loser.", positiveResponses),
				new SingleStage("Leave me alone.", positiveResponses),
				new SingleStage("Nobody understands.", positiveResponses),
				new SingleStage("I always lose.", positiveResponses),
				new SingleStage("I don't care about life.", positiveResponses) };
		resetContent();
	}
	
	
	
	

	public Boolean hasNext() {
		
		for (int i = 0; i<remainingLetters.length();i++)
		{
			if (DRAWABLE_LETTERS.contains(remainingLetters.substring(i, i+1)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	// returns a length 2 string with the first element being the in between stuff
	// and the second element containing the actual letter
	public String[] next() {
		for (int i = 0 ; i < remainingLetters.length(); i++)
		{
			if (DRAWABLE_LETTERS.contains(remainingLetters.substring(i, i+1)))
			{
				String currentLetter = remainingLetters.substring(i,i+1);
				String nonLetters = remainingLetters.substring(0,i);
				remainingLetters = remainingLetters.substring(i+1,remainingLetters.length());
				return new String[]{nonLetters,currentLetter};
			}
		}
		// Hopefully I will never have to see this if I check hasNext first.
		return new String[] {"Oh No","?"};
	}
	
	// picks out a single stage word and corresponding conclusion word
	public void resetContent() {
		int item = new Random().nextInt(allContent.length);
		SingleStage currentStage = allContent[item];
		currentStageWords = currentStage.stageWords;
		currentConclusionWords = currentStage.pickConclusion();
		int flipFlop = new Random().nextInt(2);
		isResponse = (flipFlop == 1);
		remainingLetters = new String(currentStageWords);
	}
	
	private class SingleStage {
		public String stageWords;
		public String[] conclusionWords;
		
		SingleStage(String n_singleStageWords,String[] n_allConclusionWords)
		{
			stageWords = n_singleStageWords;
			conclusionWords = n_allConclusionWords;
		}
		
		public String pickConclusion()
		{
			int item = new Random().nextInt(conclusionWords.length);
			return conclusionWords[item];
		}
	}
}