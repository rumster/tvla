package tvla.diffUtility;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.exceptions.TVLAException;
import tvla.io.CommentToTVS;
import tvla.io.DOTMessage;
import tvla.io.StructureToDOT;
import tvla.io.StructureToTVS;
import tvla.transitionSystem.Location;
import tvla.util.Pair;

/** Compares two TVLA outputs.
 * @author Roman Manevich.
 * @since 19.12.2000 Initial creation.
 */
public class OutputComparator {
	protected String tvsOutputFile;
	protected String dotOutputFile;
	protected PrintStream tvsOutputStream;
	protected PrintStream dotOutputStream;
	
	public void setTVSOutputFile(String filename) {
		tvsOutputFile = filename;
	}
	
	public void setDotOutputFile(String filename) {
		dotOutputFile = filename;
	}

	/** Compares the locations and prints the results to the specified files.
	 */
	public boolean compareLocationSets(Collection refLocations,
									   Collection newLocations,
									   String refFile,
									   String testFile) {
		boolean foundDifferences = false;
		try	{
			if (refLocations.size() != newLocations.size()) {
				printErrorMessageForDifferentCollections();
				throw new TVLAException("Different location sets!");
			}

			LocationComparatorByLabel locComparator = new LocationComparatorByLabel();
			Iterator refLocIterator = refLocations.iterator();

			while (refLocIterator.hasNext()) {
				Location refLoc = (Location)refLocIterator.next();
				Location newLoc = (Location)
								   tvla.util.Find.findEqual(newLocations,
															refLoc,
															locComparator);
				if (newLoc == null) {
					printErrorMessageForDifferentCollections();
					throw new TVLAException("Different location sets!");
				}
				ArrayList diffRef = new ArrayList();
				ArrayList diffNew = new ArrayList();
				StructureCollectionsDiff differentiator = new StructureCollectionsDiff();
				differentiator.diff(refLoc.structures , newLoc.structures , diffRef, diffNew);
				printDifferences(refLoc.label(), diffRef, diffNew, refFile, testFile);
				if (diffNew.isEmpty() == false || diffRef.isEmpty() == false)
					foundDifferences = true;
			}
			return foundDifferences;
		}
		finally {
			cleanup();
		}
	}

	protected void printErrorMessageForDifferentCollections() {
		createStreams(); // reopen streams, to erase all information in the streams.
		if (tvsOutputStream != null)
			tvsOutputStream.println(CommentToTVS.defaultInstance.convert("Error: Different location sets!"));
		if (dotOutputStream != null)
			dotOutputStream.println(DOTMessage.defaultInstance.convert("Error: Different location sets!"));
	}
	
	protected void printDifferences(String locationName, Collection diffRef, Collection diffNew,
									String refFile, String testFile) {
		if (diffRef.isEmpty() && diffNew.isEmpty()) // no differences
			return;

		// Only creating streams/files, if there are differences.
		if (tvsOutputStream == null && dotOutputStream == null) {
			createStreams();
		}

		Iterator structureIter = null;
		
		String title = "diff__" + refFile + "__" + testFile;
		if (!diffRef.isEmpty()) {
			if (tvsOutputStream != null)
				tvsOutputStream.println(CommentToTVS.defaultInstance.convert("Differences for reference location " +
											     locationName));
			if (dotOutputStream != null) {
				Pair titleMessage = new Pair(title, "Differences for reference location " + locationName);
				dotOutputStream.println(DOTMessage.defaultInstance.convert(titleMessage));
			}
			structureIter = diffRef.iterator();
			while (structureIter.hasNext()) {
				HighLevelTVS structure = (HighLevelTVS)structureIter.next();
				if (tvsOutputStream != null)
					tvsOutputStream.println(StructureToTVS.defaultInstance.convert(structure) + "\n");
				if (dotOutputStream != null)
					dotOutputStream.println(StructureToDOT.defaultInstance.convert(structure));
			}
			if (tvsOutputStream != null)
				tvsOutputStream.println();
			if (dotOutputStream != null)
				dotOutputStream.println();
		}

		if (!diffNew.isEmpty()) {
			if (tvsOutputStream != null)
				tvsOutputStream.println(CommentToTVS.defaultInstance.convert("Differences for test location " +
											     locationName));
			if (dotOutputStream != null) {
				Pair titleMessage = new Pair(title, "Differences for test(new) location " + locationName);
				dotOutputStream.println(DOTMessage.defaultInstance.convert(titleMessage));
			}
			structureIter = diffNew.iterator();
			while (structureIter.hasNext()) {
				HighLevelTVS structure = (HighLevelTVS)structureIter.next();
				if (tvsOutputStream != null)
					tvsOutputStream.println(StructureToTVS.defaultInstance.convert(structure) + "\n");
				if (dotOutputStream != null)
					dotOutputStream.println(StructureToDOT.defaultInstance.convert(structure));
			}
			if (tvsOutputStream != null)
				tvsOutputStream.println();
			if (dotOutputStream != null)
				dotOutputStream.println();
		}
	}
	
	protected void createStreams() {
		try {
			if (tvsOutputFile != null && tvsOutputStream == null)
				tvsOutputStream = new PrintStream(new FileOutputStream(tvsOutputFile));
			if (dotOutputFile != null && dotOutputStream == null)
				dotOutputStream = new PrintStream(new FileOutputStream(dotOutputFile));
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	protected void cleanup() {		
		tvsOutputFile = null;
		dotOutputFile = null;
		
		if (tvsOutputStream != null)
			tvsOutputStream.close();
		tvsOutputStream = null;
		
		if (dotOutputStream != null)
			dotOutputStream.close();
		dotOutputStream = null;
	}
}
