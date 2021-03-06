package lanreversi;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import static lanreversi.JReversi.*;

//Данный класс реализует локальную игру
public class LocalGame extends Thread {

    //Игровое поле
    private int[][] m;

    //Константы для представления содержимого ячеек игрового поля
    private static final int PLAYER = -1;
    private static final int COMPUTER = 1;
    private static final int EMPTY = 0;

    //Максимальная и минимальная глубина перебора, используемые при поиске очередного хода
    private static final int MAX_DEPTH = 7;
    private static final int MIN_DEPTH = 6;

    //Параметры поиска ходов
    private static final int MAX_SEEK = 1;    //Поиск максимума (хода с максимальной оценкой)
    private static final int MIN_SEEK = 2;    //Поиск минимума (хода с минимальной оценкой)

    private Coord playerStroke = null;    //Координаты клетки, в которую походил игрок

    //Вспомогательные переменные для представления координат
    private int xStroke;
    private int yStroke;

    //Вспомогательный список для представления наборов координат
    private LinkedList<Coord> l = new LinkedList<>();

    //Вспомогательная переменная для представления координат отдельной ячейки
    private Coord coordMaxRate;

    //В конструктор передается rows - количество строк на игровом поле, cols - количество столбцов на игровом поле
    public LocalGame(int rows, int cols) {
        m = new int[rows][cols];
    }

    @Override
    public void run() {
        //Сбрасываем результаты прошлой игры
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gui.clearBoard();
                    gui.setText1(playerName);
                    gui.setText2("2");
                    gui.setText3("2");
                    gui.setText4("Компьютер");
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
        }
        int rows = m.length;
        int cols = m[0].length;
        m[rows / 2 - 1][cols / 2 - 1] = PLAYER;
        m[rows / 2][cols / 2] = PLAYER;
        m[rows / 2][cols / 2 - 1] = COMPUTER;
        m[rows / 2 - 1][cols / 2] = COMPUTER;

        //Объявляем внутренние вспомогательные переменные
        //Признаки отсутствия доступных ходов. Если оба равны true, то игра окончена
        boolean isPlayerCellListEmpty = false;        //Если равна true, нет доступных ходов у игрока
        boolean isComputerCellListEmpty = false;      //Если равна true, нет доступных ходов у компьютера

        //Основной цикл, реализующий игру
        while (true) {
            //Первый этап ищем ячейки, в которые может походить игрок
            l = getAvailableCellList(m, PLAYER);
            isPlayerCellListEmpty = l.isEmpty();
            if (!isPlayerCellListEmpty) {
                //Помечаем цветом доступные игроку клетки
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.setEnabledCells(l);
                        }
                    });
                } catch (InvocationTargetException ex) {
                } catch (InterruptedException ex) {
                    return;
                }
                //Получаем ход игрока
                synchronized (this) {
                    playerStroke = null;
                    while (true) {
                        if (playerStroke != null) {
                            xStroke = playerStroke.x;
                            yStroke = playerStroke.y;
                            break;
                        }
                        try {
                            wait(50);
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                }
                //Обрабатываем ход игрока
                //Отключаем доступность ячеек и отображаем ход игрока
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.setEnabledCells(null);
                            gui.clearBackgroundColors();
                            Coord coordStroke=new Coord(yStroke, xStroke);
                            gui.setS1Background(coordStroke);
                            gui.setPlayerChecker(coordStroke);
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
                //Получаем список ячеек, которые перевернутся в результате хода игрока
                l = getRevertCellsList(m, PLAYER, yStroke, xStroke);
                //Фиксируем изменения на игровом поле после хода игрока
                m = getNextMatr(m, PLAYER, yStroke, xStroke);
                //Отображаем количество набранных игроком и компьютером очков на табло
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.setText2("" + getScore(m, PLAYER));
                            gui.setText3("" + getScore(m, COMPUTER));
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
                //Переворачиваем фишки на экране
                try {
                    showChekersList(l, PLAYER);
                } catch (InterruptedException ex) {
                    return;
                }
            }

            //Второй этап - поиск хода компьютера
            l = getAvailableCellList(m, COMPUTER);
            isComputerCellListEmpty = l.isEmpty();
            if (!isComputerCellListEmpty) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.setText4("Компьютер (думаю...)");
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ex) {
                    return;
                }
                //Ищем ход с максимальным рейтингом
                int maxRate = Integer.MIN_VALUE; //Переменная для хранения максимального рейтинга, достижимого из текущего набора доступных ходов
                int rate;                        //Переменная для хранения рейтинга текущего исследуемого хода
                int currentDepth=0;              //Текущая глубина перебора
                coordMaxRate = null;             //Координаты хода с максимальным рейтингом
                if((l.size()>1) & (l.size()<6))currentDepth=MAX_DEPTH;
                if(l.size()>=6)currentDepth=MIN_DEPTH;
                for (Coord coord : l) {
                    rate = getRate(getNextMatr(m, COMPUTER, coord.y, coord.x), COMPUTER, currentDepth, MAX_SEEK, Integer.MIN_VALUE);
                    if (rate > maxRate) {
                        maxRate = rate;
                        coordMaxRate = coord;
                    }
                }
                //Обрабатываем ход компьютера
                //Отображаем ход компьютера
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.clearBackgroundColors();
                            gui.setS1Background(coordMaxRate);
                            gui.setOpponentChecker(coordMaxRate);
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
                //Получаем список ячеек, которые перевернутся в результате хода компьютера
                l = getRevertCellsList(m, COMPUTER, coordMaxRate.y, coordMaxRate.x);
                //Отражаем изменения на игровом поле после хода игрока
                m = getNextMatr(m, COMPUTER, coordMaxRate.y, coordMaxRate.x);
                //Переворачиваем фишки на экране
                try {
                    showChekersList(l, COMPUTER);
                } catch (InterruptedException ex) {
                    return;
                }
                //Отображаем количество набранных игроком и компьютером очков на табло
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            gui.setText2("" + getScore(m, PLAYER));
                            gui.setText3("" + getScore(m, COMPUTER));
                            gui.setText4("Компьютер");
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
            }

            //Третий этап - проверка завершения работы
            if (isPlayerCellListEmpty & isComputerCellListEmpty) {
                final int playerScore = getScore(m, PLAYER);
                final int opponentScore = getScore(m, COMPUTER);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            if (playerScore > opponentScore) {
                                gui.showMsg("Вы победили!");
                            }
                            if (playerScore < opponentScore) {
                                gui.showMsg("Вы проиграли...");
                            }
                            if (playerScore == opponentScore) {
                                gui.showMsg("Ничья! Победила дружба!");
                            }
                        }
                    });
                } catch (InterruptedException ex) {
                    return;
                } catch (InvocationTargetException ex) {
                }
                return;
            }

        }

    }

    //Методы взаимодействия с пользователем
    //Метод необходим для переворачивания фишек на экране после очередного хода игрока или компьютера
    private void showChekersList(LinkedList<Coord> coordList, int n) throws InterruptedException {
        for (Coord coord : coordList) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        gui.setS2Background(coord);
                        if (n == PLAYER) {
                            gui.setPlayerChecker(coord);
                        }
                        if (n == COMPUTER) {
                            gui.setOpponentChecker(coord);
                        }
                    }
                });
            } catch (InterruptedException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
            }
            Thread.sleep(150);
        }
    }

    //Метод принимает координаты выбранной пользователем ячейки
    public void playerStroke(Coord coord) {
        synchronized (this) {
            playerStroke = coord;
        }
    }

    //Вспомогательные методы
    //Метод, реализующий рекурсивный поиск оценки очередного хода
    //На входе:
    //m - позиция на игровом поле, которую требуется оценить
    //n - цвет фишек, которые сделали ход
    //depth - текущая глубина рекурсии (если равна 0, рекурсивные вызовы прекращаются)
    //flag - флаг, указывающий, что мы ищем на верхнем уровне рекурсии: максимумальное или минимальное значение рейтинга хода
    //val - максимальное или минимальное значение рейтинга хода, найденное на верхнем уровне рекурсии
    private int getRate(int[][] m0, int n, int depth, int flag, int val){
        //Получаем размеры переданной в метод матрицы
        int rows=m0.length;
        int cols=m0[0].length;

        int rate=0;      //Рейтинг позиции

        boolean bitPlayer=false;      //Равен true, если у игрока есть ходы из позиции m0
        boolean bitComputer=false;    //Равен true, если у компьютера есть ходы из позиции m0

        //Проверяем, является ли позиция в m0 конечной
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                rate+=m0[i][j];
                if(m0[i][j]!=EMPTY)continue;
                if(!bitPlayer)bitPlayer=isCellAvailable(m0, PLAYER, i, j);
                if(!bitComputer)bitComputer=isCellAvailable(m0, COMPUTER, i, j);
            }
        }

        //Первый признак конечной позиции - отсутствие доступных ходов у обоих игроков
        if(!(bitPlayer | bitComputer)){
            return Integer.compare(rate, 0)*(rows*cols+1);
        }

        //Второй признак конечной позиции - достижение максимальной глубины перебора
        if(depth==0)return rate*Math.abs(rate);

        //Если конечная позиция не достигнута, то продолжаем рекурсивный перебор ходов. Сперва определяем, кто должен ходить следующим
        int nNext=0;
        switch(n){
            case PLAYER:{
                nNext=(bitComputer?COMPUTER:PLAYER);
                break;
            }
            case COMPUTER:{
                nNext=(bitPlayer?PLAYER:COMPUTER);
            }
        }

        //В зависимости от того, кто из игроков ходит мы будем искать максимальный (ходит компьютер) или минимальный (ходит игрок) рейтинг
        if(nNext==PLAYER)rate=Integer.MAX_VALUE;
        if(nNext==COMPUTER)rate=Integer.MIN_VALUE;

        //Получаем список доступных ходов
        LinkedList<Coord> moveList=getAvailableCellList(m0, nNext);

        //Перебираем ходы
        int rateTmp;
        for(Coord move: moveList){
            rateTmp=getRate(getNextMatr(m0, nNext, move.y, move.x), nNext, depth-1, (nNext==PLAYER?MIN_SEEK:MAX_SEEK), rate);
            if(nNext==PLAYER){
                rate=Math.min(rate, rateTmp);

                if(flag==MAX_SEEK){
                    if(rate<val)return rate;
                }
            }
            if(nNext==COMPUTER){
                rate=Math.max(rate, rateTmp);

                if(flag==MIN_SEEK){
                    if(rate>val)return rate;
                }
            }
        }

        //Возвращаем оценку текущего хода
        return rate;
    }

    //Метод возвращает список ячеек, которые перевернутся после хода y,x в матрицу m0 фишкой цвета n
    private LinkedList<Coord> getRevertCellsList(int[][] m0, int n, int y, int x) {
        LinkedList<Coord> res = new LinkedList<>();
        int rows = m0.length;
        int cols = m0[0].length;

        //Проверяем первый особый случай: ячейка выходит за пределы матрицы
        if ((y < 0) | (y >= rows) | (x < 0) | (x >= cols)) {
            return res;
        }

        //Проверяем второй особый случай: ячейка, в которую пытаемся поставить фишку не пуста
        if (m0[y][x] != EMPTY) {
            return res;
        }

        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int result = 0;
        int k;                 //Множитель
        int x0;                //Промежуточные координаты
        int y0;
        LinkedList<Coord> s = new LinkedList<>();    //Промежуточный список координат

        //Во внешнем цикле перебираем направления
        for (int t = 0; t < 8; t++) {
            k = 0;
            s.clear();
            while (true) {
                k++;
                x0 = x + k * dx[t];
                y0 = y + k * dy[t];
                if ((y0 < 0) | (y0 >= rows) | (x0 < 0) | (x0 >= cols)) {
                    s.clear();
                    break;
                }
                if (m0[y0][x0] == EMPTY) {
                    s.clear();
                    break;
                }
                if (m0[y0][x0] == n) {
                    break;
                }
                s.add(new Coord(y0, x0));
            }
            if (!s.isEmpty()) {
                res.addAll(s);
            }
        }

        //Возвращаем результат
        return res;
    }

    //Метод возвращает матрицу, в которую будет преобразована матрица m0, согласно ходу фишки цвета n в клетку y,x
    private int[][] getNextMatr(int[][] m0, int n, int y, int x) {
        //Сначала создаем копию исходной матрицы
        int[][] res;
        int r = m0.length;
        int c = m0[0].length;
        res = new int[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                res[i][j] = m0[i][j];
            }
        }

        //Проверяем первый особый случай: ячейка выходит за пределы матрицы
        if ((y < 0) | (y >= r) | (x < 0) | (x >= c)) {
            return res;
        }

        //Проверяем второй особый случай: ячейка, в которую пытаемся поставить фишку не пуста
        if (m0[y][x] != EMPTY) {
            return res;
        }

        //Проверяем третий особый случай: ячейка y,x - недоступна для хода
        if (!isCellAvailable(res, n, y, x)) {
            return res;
        }

        LinkedList<Coord> revertList = getRevertCellsList(res, n, y, x);
        for (Coord coord : revertList) {
            res[coord.y][coord.x] = n;
        }

        //Фиксируем ход
        res[y][x] = n;

        //Возвращаем результат
        return res;
    }

    //Метод подсчитывает количество очков, которые в матрице m0 набирает игрок с фишками n
    private int getScore(int[][] m0, int n) {
        int result = 0;
        int r = m0.length;
        int c = m0[0].length;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (m0[i][j] == n) {
                    result++;
                }
            }
        }
        return result;
    }

    //Метод возвращает список ячеек в матрице m0 доступных для установки фишки цвета n
    private LinkedList<Coord> getAvailableCellList(int[][] m0, int n) {
        LinkedList<Coord> l = new LinkedList<>();
        int r = m0.length;
        int c = m0[0].length;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (isCellAvailable(m0, n, i, j)) {
                    l.add(new Coord(i, j));
                }
            }
        }
        return l;
    }

    //Метод определяет доступность ячейки y,x в матрице m0 для установки фишки цвета n
    private boolean isCellAvailable(int[][] m0, int n, int y, int x) {
        int r = m0.length;
        int c = m0[0].length;
        if ((y < 0) | (y >= r) | (x < 0) | (x >= c)) {
            return false;
        }
        if (m0[y][x] != EMPTY) {
            return false;
        }

        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int k;                 //Множитель
        int x0;                //Промежуточные координаты
        int y0;
        int s;                 //Количество клеток, фишки в которых могут быть перевернуты по текущему направлению t

        //Во внешнем цикле перебираем направления
        for (int t = 0; t < 8; t++) {
            k = 0;
            s = 0;
            while (true) {
                k++;
                x0 = x + k * dx[t];
                y0 = y + k * dy[t];
                if ((y0 < 0) | (y0 >= r) | (x0 < 0) | (x0 >= c)) {
                    s = 0;
                    break;
                }
                if (m0[y0][x0] == EMPTY) {
                    s = 0;
                    break;
                }
                if (m0[y0][x0] == n) {
                    break;
                }
                s++;
            }
            if (s > 0) {
                return true;
            }
        }
        return false;
    }

}
