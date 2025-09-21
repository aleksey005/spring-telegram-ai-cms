package ru.simshp.telegramexplorer.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import ru.simshp.telegramexplorer.config.ExplorerProperties;
import ru.simshp.telegramexplorer.web.dto.MessageView;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    @Test
    void fallsBackToTextSearchWhenVectorReturnsEmptyResult() {
        ExplorerProperties props = createProps();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        MessageView fallback = new MessageView(1L, "channel", false, null, null,
                "text match", null, false, OffsetDateTime.now());

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(fallback));

        SearchService service = new TestableSearchService(props, jdbcTemplate);

        List<MessageView> results = service.search("кот", 5);

        assertThat(results).containsExactly(fallback);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowMapper.class));
        assertThat(sqlCaptor.getAllValues()).hasSize(2);
        assertThat(sqlCaptor.getAllValues().get(0)).contains("query_embedding");
        assertThat(sqlCaptor.getAllValues().get(1)).contains("ILIKE");
    }

    @Test
    void fallsBackToTextSearchWhenVectorQueryThrowsException() {
        ExplorerProperties props = createProps();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        MessageView fallback = new MessageView(2L, "channel", false, null, null,
                "other text", null, false, OffsetDateTime.now());

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenThrow(new RuntimeException("pgvector unavailable"))
                .thenReturn(List.of(fallback));

        SearchService service = new TestableSearchService(props, jdbcTemplate);

        List<MessageView> results = service.search("кот", 5);

        assertThat(results).containsExactly(fallback);
        verify(jdbcTemplate, times(2)).query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class));
    }

    private ExplorerProperties createProps() {
        ExplorerProperties props = new ExplorerProperties();
        props.getOpenai().setApiKey("test-key");
        return props;
    }

    private static class TestableSearchService extends SearchService {
        TestableSearchService(ExplorerProperties props, JdbcTemplate jdbcTemplate) {
            super(props, jdbcTemplate);
        }

        @Override
        protected String createEmbeddingLiteral(String text) {
            return "[0,0]";
        }
    }
}

