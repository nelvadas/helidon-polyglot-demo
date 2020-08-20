import csv
import sys


dnames={}
with open('/Users/enonowog/Projects/Demos/helidon-polyglot-demo/scripts/departement.csv', 'r') as file:
  reader = csv.reader(file)
  for row in reader:
    print('"'+row[1] +'":  "' +row[3]+'",')

