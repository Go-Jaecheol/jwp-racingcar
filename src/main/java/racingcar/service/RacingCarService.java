package racingcar.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import racingcar.dao.GameDao;
import racingcar.dao.ParticipatesDao;
import racingcar.dao.PlayerDao;
import racingcar.domain.Cars;
import racingcar.domain.GameManager;
import racingcar.domain.GameRound;
import racingcar.domain.RandomNumberGenerator;
import racingcar.dto.NamesAndCountRequest;
import racingcar.dto.ParticipateDto;
import racingcar.dto.PlayerDto;
import racingcar.dto.RacingCarResponse;
import racingcar.dto.ResultResponse;

@Service
public class RacingCarService {

    private final GameDao gameDao;
    private final PlayerDao playerDao;
    private final ParticipatesDao participatesDao;

    public RacingCarService(final GameDao gameDao, final PlayerDao playerDao, final ParticipatesDao participatesDao) {
        this.gameDao = gameDao;
        this.playerDao = playerDao;
        this.participatesDao = participatesDao;
    }

    public ResultResponse playGame(final NamesAndCountRequest namesAndCount) {
        final Cars cars = Cars.from(namesAndCount.getNames());
        final GameRound gameRound = new GameRound(namesAndCount.getCount());
        final GameManager gameManager = new GameManager(cars, gameRound, new RandomNumberGenerator());

        gameManager.play();

        final List<RacingCarResponse> racingCarResponses = gameManager.getResultCars();
        List<String> winnerNames = gameManager.decideWinner();
        saveGameAndPlayerAndParticipates(namesAndCount.getCount(), racingCarResponses, winnerNames);
        return convertResultResponse(racingCarResponses, winnerNames);
    }

    private ResultResponse convertResultResponse(final List<RacingCarResponse> racingCarResponses,
                                                 final List<String> winnerNames) {
        String winners = convertWinners(winnerNames);
        return new ResultResponse(winners, racingCarResponses);
    }

    private void saveGameAndPlayerAndParticipates(final int trialCount,
                                                  final List<RacingCarResponse> racingCarResponses,
                                                  final List<String> winnerNames) {
        Long gameId = gameDao.save(trialCount);
        for (RacingCarResponse racingCarResponse : racingCarResponses) {
            String carName = racingCarResponse.getName();
            int carPosition = racingCarResponse.getPosition();
            Long playerId = findOrSavePlayer(carName);
            ParticipateDto participateDto = convertParticipate(winnerNames, gameId, carName, carPosition, playerId);
            participatesDao.save(participateDto);
        }
    }

    private Long findOrSavePlayer(final String carName) {
        Optional<PlayerDto> playerDtoOptional = playerDao.findByName(carName);
        if (playerDtoOptional.isEmpty()) {
            return playerDao.save(carName);
        }
        return playerDtoOptional.orElseThrow().getId();
    }

    private ParticipateDto convertParticipate(final List<String> winnerNames, final Long gameId, final String carName, final int carPosition, final Long playerId) {
        if (winnerNames.contains(carName)) {
            return new ParticipateDto(gameId, playerId, carPosition, true);
        }
        return new ParticipateDto(gameId, playerId, carPosition, false);
    }

    private String convertWinners(final List<String> winnerNames) {
        return String.join(",", winnerNames);
    }
}
