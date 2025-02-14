# FEUP-AIAD
Repository for AIAD project
## Agent-based money distribution for ATM’s solution

ATMs sure make everyone's life easier, if you need money, you go to an ATM
and withdraw the amount of money you wish within your account regime limitations.
Sometimes, ATMs run out of money, which can be an annoying problem since the
person can’t withdraw, forcing a bank worker to refill the machine. With this project
our objective is to reduce the cost of refilling a machine by the workers by studying
the rate of money withdrawals and thus preventing that a machine runs out of
money.

| Dependent variables   |      Independant variables   |
|-----------------------|:-------------:|
| How long an ATM has no money |  Maximum withdrawal amount |
| ATM refill amount |    Maximum ATM refill amount   |
| Time for worker to travel to machine  | Rate of withdrawals |
| Time to refill a machine   | Number of available workers |

## Agents
* ATMs
* Clients
* Companies
* Workers


## Behaviours

Clients:
  
 - One Shot Behaviour: Client withdraws money from ATM if possible.

ATMs:	

 -  Generic Behaviour: ![ATMs State Machine](Docs/Images/ATM_SM.PNG)
	
Companies:

 -  Cyclic Behaviour: Notifies the closest worker, if he isn't available calls another one until one can do the job.

Workers: 

 -  One Shot Behaviour: Worker gets a job assigned, goes to the ATM to fulfill the machine.
