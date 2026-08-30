package com.acteque.terminal.marketdata.provider.tiingo.utilities;

import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.invalidResponse;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.optionalText;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.readList;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.requiredText;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

final class TiingoUtilitiesJsonParser {

  private static final TypeReference<List<SearchResultResponse>> SEARCH_RESULTS = new TypeReference<>() {};

  private TiingoUtilitiesJsonParser() {}

  static List<TiingoTickerSearchResult> parseSearchResults(String json) {
    List<SearchResultResponse> responses = readList(json, SEARCH_RESULTS, "ticker search results");
    try {
      return responses.stream().map(SearchResultResponse::toSearchResult).toList();
    } catch (RuntimeException exception) {
      throw invalidResponse("Invalid Tiingo ticker search result", exception);
    }
  }

  private record SearchResultResponse(
    String ticker,
    String name,
    String assetType,
    @JsonProperty("isActive") boolean active,
    String permaTicker,
    @JsonProperty("openFIGI") String openFigi
  ) {
    TiingoTickerSearchResult toSearchResult() {
      return new TiingoTickerSearchResult(
        requiredText(ticker, "ticker"),
        requiredText(name, "name"),
        requiredText(assetType, "assetType"),
        active,
        optionalText(permaTicker),
        optionalText(openFigi)
      );
    }
  }
}
