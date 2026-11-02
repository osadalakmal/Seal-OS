"""Trim the HackerTube export into a compact gzip JSON asset for the Browse page.

Usage: python3 -I scripts/build_hackertube_asset.py <hackertube-data dir> <output file (gzip data, use a .bin name)>

Keeps ids, titles, channels, HN-derived text and scores. YouTube API metadata is not part of the
export and is not added here.
"""
import gzip, json, sys

src, out = sys.argv[1], sys.argv[2]
tax = json.load(open(f"{src}/taxonomy.json", encoding="utf-8"))
cats = [
    {"id": c["id"], "name": c["name"], "area": a["id"], "areaName": a["name"]}
    for a in tax["areas"]
    for c in a["categories"]
]
videos = []
for line in open(f"{src}/videos.jsonl", encoding="utf-8"):
    v = json.loads(line)
    videos.append({
        "id": v["vid"],
        "t": v["title"],
        "ch": v["channel"],
        "s": v["summary"],
        "w": v["why_hn"],
        "ct": v["content_type"],
        "sl": v["shelf_life"],
        "tp": v["primary_topic"],
        "c": [c["id"] for c in v["categories"]],
        "r": v["hn"]["recommenders"],
        "y0": v["hn"]["first_year"],
        "y1": v["hn"]["last_year"],
        "sc": v["score"],
    })
with gzip.open(out, "wt", encoding="utf-8", compresslevel=9) as f:
    json.dump({"categories": cats, "videos": videos}, f, ensure_ascii=False, separators=(",", ":"))
print(len(videos), "videos,", len(cats), "categories")
