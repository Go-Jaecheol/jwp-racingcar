package racingcar.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WebGameDaoTest {

    private final WebGameDao webGameDao;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE PARTICIPATES IF EXISTS");
        jdbcTemplate.execute("DROP TABLE GAME IF EXISTS");
        jdbcTemplate.execute("DROP TABLE PLAYER IF EXISTS");

        jdbcTemplate.execute("CREATE TABLE GAME ( " +
                "id          BIGINT   NOT NULL AUTO_INCREMENT, " +
                "trial_count INT      NOT NULL, " +
                "created_at  DATETIME NOT NULL default current_timestamp, " +
                "PRIMARY KEY (id))");

        jdbcTemplate.execute("CREATE TABLE PLAYER ( " +
                "id   BIGINT      NOT NULL AUTO_INCREMENT, " +
                "name varchar(10) NOT NULL, " +
                "PRIMARY KEY (id)) ");

        jdbcTemplate.execute("CREATE TABLE PARTICIPATES ( " +
                "game_id   BIGINT  NOT NULL, " +
                "player_id BIGINT  NOT NULL, " +
                "position  INT     NOT NULL, " +
                "is_winner BOOLEAN NOT NULL default false, " +
                "PRIMARY KEY (game_id, player_id), " +
                "FOREIGN KEY (game_id) references GAME (id), " +
                "FOREIGN KEY (player_id) references PLAYER (id)) ");
    }

    @Autowired
    public WebGameDaoTest(final WebGameDao webGameDao, final JdbcTemplate jdbcTemplate) {
        this.webGameDao = webGameDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @ParameterizedTest(name = "시도 횟수를 입력받아 저장한다 - trialCount: {0}")
    @ValueSource(ints = {10, 15, 5, 30})
    void save(final int trialCount) {
        //when
        Long id = webGameDao.save(trialCount);
        //then
        String sql = "SELECT trial_count FROM GAME WHERE id = ?";
        assertThat(jdbcTemplate.queryForObject(sql, Integer.class, id)).isEqualTo(trialCount);
    }
}
