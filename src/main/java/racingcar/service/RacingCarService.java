package racingcar.service;

import org.springframework.stereotype.Service;
import racingcar.dao.GameDao;
import racingcar.dao.ParticipatesDao;
import racingcar.dao.PlayerDao;
import racingcar.domain.GameManager;
import racingcar.domain.RandomNumberGenerator;
import racingcar.dto.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class RacingCarService {

    public static final String SEPARATOR = ",";

    private final GameDao gameDao;
    private final PlayerDao playerDao;
    private final ParticipatesDao participatesDao;

    public RacingCarService(final GameDao gameDao, final PlayerDao playerDao, final ParticipatesDao participatesDao) {
        this.gameDao = gameDao;
        this.playerDao = playerDao;
        this.participatesDao = participatesDao;
    }
    public ResultResponse playGame(final NamesAndCountRequest namesAndCount) {
        final GameManager gameManager = new GameManager(new RandomNumberGenerator());
        
        int trialCount = createGame(namesAndCount, gameManager);
        createPlayers(namesAndCount, gameManager);

        List<CarStatusResponse> carStatusResponses = new ArrayList<>();
        while (!gameManager.isEnd()) {
            carStatusResponses = gameManager.playGameRound();
        }
        GameResultResponse gameResultResponse = gameManager.decideWinner();
        List<String> winnerNames = gameResultResponse.getWinnerNames();
        
        Long gameId = gameDao.save(trialCount);
        for (CarStatusResponse carStatusResponse : carStatusResponses) {
            String carName = carStatusResponse.getCarName();
            int carPosition = carStatusResponse.getCarPosition();
            Long playerId = playerDao.save(carName);
            ParticipateDto participateDto = convertParticipate(winnerNames, gameId, carName, carPosition, playerId);
            participatesDao.save(participateDto);
        }
        
        String winners = convertWinners(winnerNames);
        List<RacingCarResponse> racingCarResponses = convertRacingCars(carStatusResponses);

        return new ResultResponse(winners, racingCarResponses);
    }

    private ParticipateDto convertParticipate(final List<String> winnerNames, final Long gameId, final String carName, final int carPosition, final Long playerId) {
        if (winnerNames.contains(carName)) {
            return new ParticipateDto(gameId, playerId, carPosition, true);
        }
        return new ParticipateDto(gameId, playerId, carPosition, false);
    }

    private int createGame(final NamesAndCountRequest namesAndCount, final GameManager gameManager) {
        int trialCount = namesAndCount.getCount();
        GameRoundRequest gameRoundRequest = new GameRoundRequest(trialCount);
        gameManager.createGameRound(gameRoundRequest);
        return trialCount;
    }

    private void createPlayers(final NamesAndCountRequest namesAndCount, final GameManager gameManager) {
        List<String> carNames = List.of(namesAndCount.getNames().split(SEPARATOR));
        CarNamesRequest carNamesRequest = new CarNamesRequest(carNames);
        gameManager.createCars(carNamesRequest);
    }

    private String convertWinners(final List<String> winnerNames) {
        return String.join(",", winnerNames);
    }

    private List<RacingCarResponse> convertRacingCars(final List<CarStatusResponse> carStatusResponses) {
        List<RacingCarResponse> racingCarsResponses = new ArrayList<>();
        for (CarStatusResponse carStatusResponse : carStatusResponses) {
            racingCarsResponses.add(new RacingCarResponse(carStatusResponse.getCarName(), carStatusResponse.getCarPosition()));
        }
        return racingCarsResponses;
    }
}
