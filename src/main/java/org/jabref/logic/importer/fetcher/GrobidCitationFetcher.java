package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidCitationFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcher.class);
    private static final String GROBID_URL = "http://grobid.jabref.org:8070";
    private ImportFormatPreferences importFormatPreferences;
    private GrobidService grobidService;

    public GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.grobidService = new GrobidService(GROBID_URL);
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result.
     * Takes a while, since the server has to look up the entry.
     * @return A BibTeX-String if extraction is successful and an empty String otherwise.
     */
    private String parseUsingGrobid(String plainText) {
        try {
            return grobidService.processCitation(plainText, GrobidService.ConsolidateCitations.WITH_METADATA);
        } catch (IOException e) {
            LOGGER.debug("Could not process citation", e);
            return "";
        }
    }

    private Optional<BibEntry> parseBibToBibEntry(String bibtexString) {
        try {
            return BibtexParser.singleFromString(bibtexString,
                    importFormatPreferences, new DummyFileUpdateMonitor());
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<BibEntry> performSearch(String query) {
        List<String> plainReferences = Arrays.stream( query.split( "\\r\\r+|\\n\\n+|\\r\\n(\\r\\n)+" ) )
              .map(String::trim)
              .filter(str -> !str.isBlank())
              .collect(Collectors.toCollection(ArrayList::new));
        if (plainReferences.isEmpty()) {
            return Collections.emptyList();
        } else {
            return plainReferences.stream()
                  .map(reference -> parseBibToBibEntry(parseUsingGrobid(reference)))
                  .flatMap(Optional::stream)
                  .collect(Collectors.toList());
        }
    }

    @Override
    public String getName() {
        return "GROBID";
    }
}
