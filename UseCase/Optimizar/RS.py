# -*- coding: utf-8 -*-
"""
@author:  shusuke shigenaka
"""

import csv
import random
import sys
import time
import numpy as np
import math
from multiprocessing import Pool
from multiprocessing import Process

START_CYCLE = 0
FINISH_CYCLE = 1
POP_SIZE = 40
NAIVE = 1
RATIONAL = 1
RUBY = 1
PEOPLE_SIZE_RATE = 0.01
POOL_SIZE = 20
FILEPATH = ""

SIGMA_RANGE = [3.0,100.0]
MU_RANGE = [0.0, 300.0]
PI_RANGE = [0.0, 1.0]

#python RS.py 0 50 40 1 1 1

if len(sys.argv) == 8:
    START_CYCLE = int(sys.argv[1])
    FINISH_CYCLE = int(sys.argv[2])
    POP_SIZE = int(sys.argv[3])
    NAIVE = int(sys.argv[4])
    RATIONAL = int(sys.argv[5])
    RUBY = int(sys.argv[6])
    FILEPATH = sys.argv[7]
else :
    sys.exit(1)

def random_range(MIN, MAX):
    return random.random()*(MAX - MIN) + MIN

def csv_append(csv_colmuns,colmuns_name,agent_size):
    for i in range(agent_size):
        csv_colmuns.append("sigma_"+colmuns_name+str(i))
        csv_colmuns.append("mu_"+colmuns_name+str(i))
        csv_colmuns.append("pi_"+colmuns_name+str(i))

def people_size_check(current_pop, sigma, mu, pi, dimension, t):
    cp = current_pop
    result = 0.0
    for d in range(dimension):
        A = (t - mu[cp][d])**2.0
        B = (sigma[cp][d]**2.0)*2.0
        C = math.sqrt((sigma[cp][d]**2)*math.pi*2.0)
        result = math.exp(-1.0 * A / B) / C
    return result

def people_size_check_multi(args):
    return people_size_check(*args)

def norm_value_multi(pool, current_pop, sigma, mu, pi, dimension):
    sum_people = 0.0
    max_people = 0.0
    for res in pool.map(people_size_check_multi,[[current_pop, sigma, mu, pi, dimension, t] for t in range(300)]):
        sum_people += res
        max_people = max(max_people, res)
    return max_people * 1.0 / sum_people

def RS(CURRENT_CYCLE):
    sigma = np.zeros((POP_SIZE, NAIVE + RATIONAL + RUBY))
    mu = np.zeros((POP_SIZE, NAIVE + RATIONAL + RUBY))
    pi = np.zeros((POP_SIZE, NAIVE + RATIONAL + RUBY))
    current_pop = 0
    dimension = NAIVE + RATIONAL + RUBY
    csv_colmuns = []
    csv_append(csv_colmuns,"naive",NAIVE)
    csv_append(csv_colmuns,"rational",RATIONAL)
    csv_append(csv_colmuns,"ruby",RUBY)
    pool = Pool(POOL_SIZE)

    while current_pop < POP_SIZE:
        sum_pi = 0.0
        # set value pi
        for j in range(dimension):
            pi[current_pop][j] = random_range(PI_RANGE[0],PI_RANGE[1])
            if(j < dimension - 1):
                sum_pi = sum_pi + pi[current_pop][j]
        
        # sum_pi = 1.0
        if(PI_RANGE[1] - sum_pi > 0):
            pi[current_pop][dimension - 1] = PI_RANGE[1] - sum_pi
        else :
            continue

        # set value other norm
        for j in range(dimension):
            sigma[current_pop][j] = random_range(SIGMA_RANGE[0],SIGMA_RANGE[1])
            mu[current_pop][j] = random_range(MU_RANGE[0],MU_RANGE[1])

        # check people size for one sepalate
        rate = norm_value_multi(pool, current_pop, sigma, mu, pi, dimension)
        if rate <= PEOPLE_SIZE_RATE:
            print(CURRENT_CYCLE, current_pop)
            csv_datasets = []
            for j in range(dimension):
                csv_datasets.append(sigma[current_pop][j])
                csv_datasets.append(mu[current_pop][j])
                csv_datasets.append(pi[current_pop][j])
            with open(FILEPATH+'population_'+str(current_pop)+'.csv', 'w') as f:
                writer = csv.writer(f, lineterminator='\n')
                writer.writerow(csv_colmuns)
                writer.writerow(csv_datasets)
            current_pop = current_pop + 1
    pool.close()
    pool.terminate()


if __name__ == "__main__":
    for i in range(START_CYCLE,FINISH_CYCLE):
        RS(i)
