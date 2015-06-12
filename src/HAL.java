

import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;

import support.Command;
import enumerate.Action;
import enumerate.AttackAction;
import enumerate.Position;
import enumerate.State;
import structs.*;
import structs.CharacterData;
import gameInterface.AIInterface;

public class HAL implements AIInterface {

	//SET KNN DETAILS
	private static final int K_DISTANCE = 40;
	private static final double K_THRESHOLD = 10;
	private static final int THRESHOLD = 9;

	boolean p;

	GameData gd;
	Key inputKey;
	FrameData fd;
	
	CharacterData my;
	CharacterData opp;

	MotionData oppMotion = new MotionData();

	String myAction = null;
	CharacterData characterdata;
	CharacterData characterdata2;
	
	Position pos;
	
	Deque<ActData> oppActData_GG;
	Deque<ActData> oppActData_GA;
	Deque<ActData> oppActData_AG;
	Deque<ActData> oppActData_AA;
	
	Deque<Action> G_Act;
	Deque<Action> A_Act;
	
	Deque<Action> myAct;
	Deque<Action> oppAct;
	int[] checkAct;
	
	Deque<KeyData> inputLog;
	
	Command cc;
	
	Action preOppAct;
	Action nowOppAct;
	
	int preRound;
	int nowRound;
	int skillcounter=0;
	int punchcounter=0;
	int attack_type = 0;
	int X=0;
	
	long time;
	


	/////////////////////////////////
	//CALLED ONCE TO SET UP VARIABLES
	/////////////////////////////////
	@Override
	public synchronized int initialize(GameData gameData, boolean playerNumber) {
		gd = gameData;
		p = playerNumber;
		
		inputKey = new Key();
		fd = new FrameData();
		cc = new Command();
		
		preOppAct = Action.NEUTRAL;
		nowOppAct = Action.NEUTRAL;
		
		preRound = 0;
		nowRound = 0;

		this.inputLog = new LinkedList<KeyData>();
		this.oppActData_GG = new LinkedList<ActData>();
		this.oppActData_GA = new LinkedList<ActData>();
		this.oppActData_AG = new LinkedList<ActData>();
		this.oppActData_AA = new LinkedList<ActData>();
		this.myAct = new LinkedList<Action>();
		this.oppAct = new LinkedList<Action>();
		checkAct = new int[EnumSet.allOf(Action.class).size()];
				
		return 0;
	}



	/////////////////////////////////
	//After initialization, the process will continue as
	//1.	getInformaiton
	//2.	processing
	//3.	input
	//for each frame.
	/////////////////////////////////
	@Override
	public synchronized void getInformation(FrameData frameData) {
		time = System.currentTimeMillis();
		fd = frameData;
		cc.setFrameData(fd, p);
		if(p){
			my = fd.getP1();
			opp = fd.getP2();
		}else{
			my = fd.getP2();
			opp = fd.getP1();
		}
		
		for(int i = 0 ; i < EnumSet.allOf(Action.class).size() ; i++){ 
			checkAct[i] = 0;
		}
		
		nowRound = fd.getRound();
		if(nowRound != preRound && nowRound != 0 && nowRound%3 == 0){
			oppActData_GG.clear();
			oppActData_GA.clear();
			oppActData_AG.clear();
			oppActData_AA.clear();
			System.out.println("Clear");
		}
	}
	
	
	
	@Override
	public synchronized void processing() {
		if(!fd.emptyFlag && fd.getRemainingTime() > 0){	
			//boolean temp;
			characterdata2=cc.getEnemyCharacter();
			characterdata=cc.getMyCharacter();
			int posx=characterdata.getX();
			int posx2=characterdata2.getX();
			X = cc.getDistanceX();
			myAction = cc.getMyCharacter().getAction().name();
			Action oppAct1 = cc.getEnemyCharacter().getAction();
			MotionData oppMotion = new MotionData();
			if (p)
				oppMotion = gd.getPlayerTwoMotion().elementAt(oppAct1.ordinal());
				
			else
				oppMotion = gd.getPlayerOneMotion().elementAt(oppAct1.ordinal());
			attack_type = oppMotion.getAttackType();
			
			

			/////////////////////////////////
			//collect information
			/////////////////////////////////		
			nowOppAct = opp.getAction();			
			if(gd.getPlayerTwoMotion().elementAt(opp.getAction().ordinal()).getFrameNumber() == opp.getRemainingFrame()){
				try{					
					AttackAction.valueOf(nowOppAct.name());
					if(my.isFront()){
						ActData act = new ActData(opp.getX()-my.getX(),opp.getY()-my.getY(),nowOppAct);
						setOppAttackData(act);
					}else{
						ActData act = new ActData(my.getX()-opp.getX(),my.getY()-opp.getY(),nowOppAct);
						setOppAttackData(act);
					}
				}catch (Exception e){
				}
			}
			
			
			//SET MY POSITION 
			setPosition();
						
			
			if (cc.getskillFlag()) {
				inputKey = cc.getSkillKey();

			} else {
			
				inputKey.empty();
				cc.skillCancel();


				///////////////////////////////
				//SPECIAL SKILLS & COMBO MOVES
				///////////////////////////////		
				//special attack is the first choice
				if (cc.getMyEnergy() >= 400 && !oppAct.equals("AIR") && !oppMotion.getMotionName().equalsIgnoreCase("DOWN")) {
					cc.commandCall("STAND_D_DF_FC");
					//reset limit skill to 0 
					skillcounter=0;
				}
				//attack with skill that use energy //slide attack
				else if(X<220&&X>150&&cc.getMyEnergy()<400&&cc.getMyEnergy()>50&&skillcounter!=15&&!cc.getEnemyCharacter().getState().name().equals("AIR")){
					cc.commandCall("STAND_F_D_DFB");
					//limit skill use
					skillcounter++;
				}
				//combo with air moves
				else if(cc.getMyCharacter().getState().name().equals("AIR")){
					cc.commandCall("A");
				}				
				//retreat if down
				else if(cc.getMyCharacter().getState().name().equals("DOWN")){
					if(my.getX() > 0)
						cc.commandCall("7 _ C");//GUARD JUMP
					else
						cc.commandCall("9 _ C");//GUARD JUMP
				}
				
				
				//if enemy has high energy play carefully
				else if(cc.getEnemyCharacter().getEnergy() >= 350){
					if(my.getX() > 0)
						cc.commandCall("7 _ C");//GUARD JUMP
					else
						cc.commandCall("9 _ C");//GUARD JUMP
				}
				
				
				//stops agent getting trapped in kick loops
				else if(cc.getEnemyCharacter().getAction().name().equals("STAND_FB")){
					if(my.getX() > 0)
						cc.commandCall("7 _ C");//GUARD JUMP
					else
						cc.commandCall("9 _ C");//GUARD JUMP
				}
				
				//stops agent getting trapped
				else if(my.getX()==710||my.getX()==-170&&cc.getMyEnergy()<400){
					cc.commandCall("FOR_JUMP");
				}
				
				//attack with normal skill no energy use
				else if(X>63&&X<150&&cc.getMyEnergy()<400){
					cc.commandCall("STAND_FB");
				}
			
				

				///////////////////////////////
				//PREDICTION
				///////////////////////////////
				else if(calculateActDistance(getOppAttackData(),opp.getX()-my.getX(),opp.getY()-my.getY())){
					//counter special attack
					if (oppAct.getLast().name().equals("STAND_D_DF_FC")) {
						cc.commandCall("FOR_JUMP");
					}	
					else if (oppAct.getLast().name().equals("STAND_D_DF_FC")) {
						cc.commandCall("CROUCH_FB");
					} 
					else if(oppAct.getLast().name().startsWith("AIR")&&X<230){
						cc.commandCall("AIR_UB");
					} 	//default prediction move
					else
						cc.commandCall("STAND_FB");
										
					//cc.commandCall(oppAct.getLast().toString());
				}

				


				///////////////////////////////
				//CLOSE COMBAT
				///////////////////////////////
				//punch in very close range
				else if(X<35){
					if(punchcounter!=5){
					cc.commandCall("STAND_B");
					punchcounter++;
					}//combo with punch
					else {
						cc.commandCall("CROUCH_FB");
						punchcounter=0;
					}
				}
				//throw
				 else if (X>32&&X <63&&!cc.getEnemyCharacter().getAction().name().startsWith("AIR")) {
					cc.commandCall("THROW_B");
				}//combo with throw
				 else if (cc.getEnemyCharacter().getAction().name().equals("THROW_SUFFER")&&cc.getMyEnergy()>=30) {
					cc.commandCall("STAND_D_DF_FB ");
				}
						
				
				
				
				/////////////////////////////////
				//defend tactics
				//attack type 
				//1=high
				//2=middle
				//3=low 
				//4=throw
				/////////////////////////////////
				//high attack type
				else if (attack_type == 1&&X<200&&!cc.getEnemyCharacter().getAction().name().equals("STAND_D_DF_FC")) {
					cc.commandCall("CROUCH_FB");
				} 
				//middle attack type
				else if (attack_type == 2&&X<200) {
					cc.commandCall("STAND_FB");
				}
				//low attack type
				else if(attack_type == 3&&X<200&&!cc.getEnemyCharacter().getAction().name().equals("STAND_D_DF_FC")){
					cc.commandCall("CROUCH_FB");
				}
				// enemy use air skill 
				else if(cc.getEnemyCharacter().getAction().name().startsWith("AIR")&&X<230){
					cc.commandCall("AIR_UB");
				} 
				
				//jump out when at the end of corner
				else if(my.getX()==710||my.getX()==-170&&cc.getMyEnergy()<400){
					cc.commandCall("FOR_JUMP");
				}



				/////////////////////////////////
				//DEFAULT MOVES
				/////////////////////////////////
				else if(posx>posx2){
					inputKey.L =true;
				}
				else{
					inputKey.R =true;
				}
					
					

				/////////////////////////////////
				//DEBUG INFORMATION
				/////////////////////////////////				
				System.out.println("ATTACK TYPE: " + attack_type);
				System.out.println("INPUT: " + cc.getEnemyCharacter().getAction().name());
				System.out.println("My HP: " +  this.fd.getP1().getHp());
				System.out.println("Opp HP: " + this.fd.getP2().getHp());
				System.out.println("OPPONENT ATTACK: " + nowOppAct);
				System.out.println("X POSITION: " + my.getX());
				}
			}
	fin();
	}
		
	

	@Override
	public synchronized Key input() {
		return inputKey;
	}

	@Override
	public synchronized void close() {
		oppActData_GG.clear();
		oppActData_GA.clear();
		oppActData_AG.clear();
		oppActData_AA.clear();
	}
	
	private synchronized void fin(){
		preOppAct = nowOppAct;
		preRound = nowRound;
		oppAct.clear();
	}
	
	private synchronized boolean calculateActDistance(Deque<ActData> actData , int x, int y){
		int threshold = (int)(actData.size()*K_THRESHOLD + 1);
		Deque<ActData> temp = new LinkedList<ActData>();
		ActData[] array;
		
		for(Iterator<ActData> i = actData.iterator() ; i.hasNext() ; ){
			ActData act = new ActData(i.next());
			if(my.isFront()) act.setDistance((int)Math.sqrt((act.getX()-x)*(act.getX()-x)+(act.getY()-y)*(act.getY()-y)));
			else act.setDistance((int)Math.sqrt((act.getX()+x)*(act.getX()+x)+(act.getY()+y)*(act.getY()+y)));
			if( act.getDistance() < K_DISTANCE) temp.add(act);
		}
		
		if(temp.size() < Math.min(threshold,THRESHOLD)) return false;		
		
		array = new ActData[temp.size()];
		array = actSort(temp);
		setOppAct(array, Math.min(threshold,THRESHOLD));
		
		return true;
	}
	
	
	private synchronized ActData[] actSort(Deque<ActData> actData){
		ActData[] array = new ActData[actData.size()];		
		for(int i = 0 ; i < array.length ; i ++){
			array[i] = new ActData(actData.pop());
		}
		sort(array);
		
		return array;
	}
	
	private synchronized void merge(ActData[] a1,ActData[] a2,ActData[] a){
		int i=0,j=0;
		while(i<a1.length || j<a2.length){
			if(j>=a2.length || (i<a1.length && a1[i].getDistance()<a2[j].getDistance())){
				a[i+j].setMenber(a1[i]);
				i++;
			}
			else{
				a[i+j].setMenber(a2[j]);
				j++;
			}
		}
	}

	private synchronized void mergeSort(ActData[] a){
		if(a.length>1){
			int m=a.length/2;
			int n=a.length-m;
			ActData[] a1=new ActData[m];
			ActData[] a2=new ActData[n];
			for(int i=0;i<m;i++) a1[i] = new ActData(a[i]);
			for(int i=0;i<n;i++) a2[i] = new ActData(a[m+i]);
			mergeSort(a1);
			mergeSort(a2);
			merge(a1,a2,a);
		}
	}

	private synchronized void sort(ActData[] a){
		mergeSort(a);
	}
	
	
	private synchronized void setOppAct(ActData[] array,int threshold){
		Action[] subAct = Action.values();
		
		int max = 1;
		
		for(int i = 0 ; i < threshold ; i++){ //COUNT THE NEIGHBOURS VOTES
			checkAct[array[i].getAct().ordinal()] ++;
		}
		
		for(int i = 0 ; i < EnumSet.allOf(Action.class).size() ; i++){ //SET OPP BASED ON VOTE
			if(checkAct[i] > max){
				oppAct.clear();
				oppAct.add(subAct[i]);
				max = checkAct[i];
			}
			else if(checkAct[i] == max){
				oppAct.add(subAct[i]);
			}
		}
	}
	
	
	private synchronized void setPosition(){
		if(my.getState() == State.AIR){
			if(opp.getState() == State.AIR) pos = Position.Air_Air;
			else pos = Position.Air_Ground;
		}else{
			if(opp.getState() == State.AIR) pos = Position.Ground_Air;
			else pos = Position.Ground_Ground;
		}
	}
	
	
	private synchronized Deque<ActData> getOppAttackData(){
		switch(pos){
		case Air_Air: return oppActData_AA;
		case Air_Ground: return oppActData_AG;
		case Ground_Air: return oppActData_GA;
		case Ground_Ground: return oppActData_GG;
		}
		return oppActData_GG;
	}
	
	
	private synchronized void setOppAttackData(ActData act){
		switch(pos){
		case Air_Air: oppActData_AA.add(act); break;
		case Air_Ground: oppActData_AG.add(act); break;
		case Ground_Air: oppActData_GA.add(act); break;
		case Ground_Ground :oppActData_GG.add(act);break;
		}
	}


	@Override
	public String getCharacter() {
		// TODO Auto-generated method stub
		return CHARACTER_LUD;
	}
}
