import urllib2
import xmltodict
import json
import boto3


ROUTES_ENDPOINT = "http://webservices.nextbus.com/service/publicXMLFeed?command=routeList&a=sf-muni"
STOPS_ENDPOINT = "http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=sf-muni&r={}"


def get_routes():
    result = urllib2.urlopen(ROUTES_ENDPOINT)
    dom = xmltodict.parse(result.read())
    return dom['body']['route']


def get_db_table():
    dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
    return dynamodb.Table('routes')


def load_table(table, routes):
    for route in routes:
        stops = get_stops(route['@tag'])
        stops = [{
            "stopId":stop['@stopId'],
            "tag":stop['@tag'],
            "title":stop['@title'],
        } for stop in stops]

        table.put_item(
            Item={
                'tag':route['@tag'],
                'title':route['@title'],
                'stops':stops
            }
        )


def get_stops(route_tag):
    result = urllib2.urlopen(STOPS_ENDPOINT.format(route_tag))
    dom = xmltodict.parse(result.read())
    stops = dom['body']['route']['stop']
    return stops


if __name__ == '__main__':
    routes = get_routes()
    table = get_db_table()
    load_table(table, routes)
