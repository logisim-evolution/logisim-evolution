# Importing BeautifulSoup class from the bs4 module 
from bs4 import BeautifulSoup 
from datetime import datetime
import os


def auto(file):
    # now = datetime.now()
    # datetime.strptime  strptime
    formatted_time=datetime.now().strftime("%Y-%M-%dT%H:%M:%S")
    print(formatted_time)
    # with open("index.html") as fp:
    #     soup = BeautifulSoup(fp)
    soup = BeautifulSoup(open(file, encoding='utf8'), 'html.parser')
    print(soup)
        
        
        
    # soup.append("meta")
    # tag = soup.new_tag("meta")
    # tag.string = "Don't"
    # soup.title.insert_before(tag)

    html = soup.html
    html['lang']="zh"

    # metas=soup.find_all('meta')
    # print(soup.find('meta')['name'])
    # print(soup.find_all('meta')[2]['name'])

    metas=soup.find_all('meta')
    count=len(metas)
    # for i in range(0,count):
    # print(soup.find_all('meta', content='en'))
        
    zhs=soup.find_all('meta', content='en')
    if zhs != []:
        zhs[0]['content']='zh'
        print(zhs)

    changeds=soup.find_all('meta', attrs={"name": "changed"})
    if changeds != []:
        changeds[0]['content']=formatted_time
        changeds[0]['translator']='gocpicnic'
        print(changeds)


    # print(count)
    # for i in range(0,len(metas)):
    #     print(metas[i]['name'])



    # for tag in soup.find_all('meta'):
    #     print(tag.attrs)
    #     print(tag.get_text())
    # print(soup)
    # print(metas)
    # fp.write()
    with open(file, 'w',encoding="utf-8") as fp:
        # write the current soup content
        fp.write(soup.prettify())

if __name__ == '__main__':
    # auto("index.html")
    base="/Users/huiyibao/interest/interest/Research/Java/logisim-evolution/src/main/resources/doc/zh/html/guide/mem"
    # filelist=[""]
    
    for root, ds, fs in os.walk(base):
        for f in fs:
            if f.endswith('.html'):
                # fullname = os.path.join(root, f)
                auto(f)
                
                # yield fullname
    