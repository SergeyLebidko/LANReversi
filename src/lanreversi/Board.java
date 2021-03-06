package lanreversi;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import static lanreversi.JReversi.*;

//В данный класс выделена группа операций, осуществляемых с клетками игрового поля
public final class Board extends JPanel{

    //Количество клеток на игровом поле
    private final int cols;    //Количество столбцов
    private final int rows;    //Количество строк

    private int playerColor=Cell.BLACK;    //Цвет фишек игрока
    private int opponentColor=Cell.WHITE;  //Цвет фишек противника
    private int currentStyle=0;            //Текущий стиль ячеек

    private final Cell[][] c;  //Клетки игрового поля

    public Board(int rows, int cols) {
        super(new GridLayout(rows, cols, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.cols=cols;
        this.rows=rows;
        c=new Cell[rows][cols];
        for(int i=0;i<rows;i++)
            for(int j=0;j<cols;j++){
                c[i][j]=new Cell(new Coord(i, j), Cell.EMPTY, 0);
                this.add(c[i][j]);
                c[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if(e.getButton()!=1)return;
                        Cell cell=(Cell)e.getSource();
                        if(cell.isEnabledCell())gui.playerStroke(cell.getCoord());
                    }

                });
            }
        clearBoard();
    }

    //Метод сбрасывает игровое поле до начальной конфигурации
    public void clearBoard(){
        for(int i=0;i<rows;i++)
            for(int j=0;j<cols;j++){
                c[i][j].setContent(Cell.EMPTY);
                c[i][j].setStyle(currentStyle);
                c[i][j].setEnabledCell(false);
                c[i][j].clearBackgroundColor();
            }
        c[rows/2-1][cols/2-1].setContent(playerColor);
        c[rows/2][cols/2].setContent(playerColor);
        c[rows/2][cols/2-1].setContent(opponentColor);
        c[rows/2-1][cols/2].setContent(opponentColor);
    }

    //Метод возвращает текущий цвет фишек игрока
    public int getPlayerColor(){
        return playerColor;
    }

    //Метод изменяет цвет фишек игрока и противника
    public void revertColor(){
        int t;
        t=playerColor;
        playerColor=opponentColor;
        opponentColor=t;
        for(int i=0;i<rows;i++)
            for(int j=0;j<cols;j++){
                if(c[i][j].isEmpty())continue;
                if(c[i][j].getContent()==playerColor){
                    c[i][j].setContent(opponentColor);
                    continue;
                }
                c[i][j].setContent(playerColor);
            }
    }

    //Метод возвращает имя текущего стиля
    public String getCurrentStyleName(){
        return Cell.getStyleNames()[currentStyle];
    }

    //Метод изменяет стиль фишек
    public void nextStyle(){
        currentStyle++;
        if(currentStyle>=Cell.getCountStyles())currentStyle=0;
        for(int i=0;i<rows;i++)
            for(int j=0;j<cols;j++)c[i][j].setStyle(currentStyle);
    }

    //Метод устанавливает в ячейку фишку игрока
    public void setPlayerChecker(Coord coord){
        int x=coord.x;
        int y=coord.y;
        if((y>=0) & (y<=rows) & (x>=0) & (x<=cols))c[y][x].setContent(playerColor);
    }

    //Метод устанавливает в ячейку фишку противника
    public void setOpponentChecker(Coord coord){
        int x=coord.x;
        int y=coord.y;
        if((y>=0) & (y<=rows) & (x>=0) & (x<=cols))c[y][x].setContent(opponentColor);
    }

    //Метод устанавливает первый дополнительный цвет фона
    public void setS1Background(Coord coord){
        int x=coord.x;
        int y=coord.y;
        if((y>=0) & (y<=rows) & (x>=0) & (x<=cols))c[y][x].setS1Background();
    }

    //Метод устанавливает второй дополнительный цвет фона
    public void setS2Background(Coord coord){
        int x=coord.x;
        int y=coord.y;
        if((y>=0) & (y<=rows) & (x>=0) & (x<=cols))c[y][x].setS2Background();
    }

    //Метод удаляет дополнительные цвета фона
    public void clearBackgroundColors(){
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                if(c[i][j].isS1Background() | c[i][j].isS2Background())c[i][j].clearBackgroundColor();
            }
        }
    }

    //Метод делает доступными для выбора игроком переданные ему ячейки. Если coord пуст или равен null, все ячейки помечаются, как недоступные
    public void setEnabledCells(List<Coord> coords){
        //Сначала сбрасываем доступность ранее отмеченных как доступные клеток
        for(int i=0;i<rows;i++)
            for(int j=0;j<cols;j++){
                c[i][j].setEnabledCell(false);
            }
        if(coords==null)return;
        if(coords.isEmpty())return;
        for(Coord c0: coords){
            c[c0.y][c0.x].setEnabledCell(true);
        }
    }

}
